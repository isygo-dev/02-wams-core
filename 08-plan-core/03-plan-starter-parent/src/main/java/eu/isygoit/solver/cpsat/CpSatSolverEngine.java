package eu.isygoit.solver.cpsat;

import com.google.ortools.Loader;
import com.google.ortools.sat.CpSolverStatus;
import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.SolverOptions;
import eu.isygoit.dto.response.CoreItem;
import eu.isygoit.dto.response.Diagnostics;
import eu.isygoit.dto.response.InfeasibilityCore;
import eu.isygoit.dto.response.Metrics;
import eu.isygoit.dto.response.ModelStats;
import eu.isygoit.dto.response.PlanningSolution;
import eu.isygoit.dto.response.Relaxation;
import eu.isygoit.dto.response.SearchStats;
import eu.isygoit.dto.response.SolveResult;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.enums.IEnumSolverStatus;
import eu.isygoit.exception.InvalidRequestException;
import eu.isygoit.exception.SolverUnavailableException;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.CompiledProblem;
import eu.isygoit.solver.EngineCapabilities;
import eu.isygoit.solver.SolveCancelledException;
import eu.isygoit.solver.SolveContext;
import eu.isygoit.solver.SolveProgress;
import eu.isygoit.solver.SolverEngine;
import eu.isygoit.solver.cpsat.explain.CpSatExplainer;
import eu.isygoit.solver.diag.CoreItems;
import eu.isygoit.solver.registry.CpSatContributorRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * The reference CP-SAT engine (canonical id {@code cp_sat}), implementing the full v2 model (design 5.3, 6, D-8 to D-12).
 * <p>
 * {@link #solve} flow: lazy native loading ({@link SolverUnavailableException} when OR-Tools cannot be loaded) -> model
 * construction by the {@link CpModelBuilder} (time and cancellation checked while building; the budget includes the build
 * time because it is measured from {@code SolveContext.startNanos}) -> search by the {@link SolveRunner} (hints, weighted /
 * sequential tiers, relaxation in one solve, deterministic mode, progress callback, stop hook registered right before the
 * solve) -> status mapping and extraction:
 * <ul>
 *   <li>OPTIMAL (all tiers proven) / FEASIBLE (time or gap limit);</li>
 *   <li>STOPPED_FEASIBLE: a stop was requested while an incumbent existed and {@code keepIncumbentOnStop} (D-11);</li>
 *   <li>INFEASIBLE: proven; the explanation pass of {@link CpSatExplainer} fills {@code Diagnostics.core} (design 6.10 B);</li>
 *   <li>UNKNOWN: nothing found (stop, time limit); a stop or the time limit during model building returns UNKNOWN as in
 *   v1; {@code MODEL_INVALID} stays the {@code INVALID_SOLVER_MODEL} error.</li>
 * </ul>
 * The result carries ticks AND instants, per tier values and model/search statistics. Qualities, constraint scores and
 * unselected-task reasons are completed by the independent validator (WP6); dropped relaxable constraints are reported as
 * preliminary {@link Relaxation}s (violationCount 0) and in {@code diagnostics.details.relaxedConstraintIds} for the
 * relaxation reporter.
 * <p>
 * Thread-safety: a stateless Spring singleton; every call uses its own model and solver. The native library is loaded
 * once under a lock and the outcome cached (a failed load is attempted once). Units: ticks and scaled objective values.
 */
@Slf4j
@Component
public class CpSatSolverEngine implements SolverEngine {

    /** Canonical engine id. */
    public static final String NAME = "cp_sat";

    private final CpModelBuilder builder;
    private final SolveRunner runner;
    private final CpSatExplainer explainer;
    private final NativeLoader nativeLoader;
    private final Object loadLock = new Object();
    private volatile boolean loaded;
    private volatile Throwable loadFailure;

    /** Seam for loading the OR-Tools native libraries (replaceable in tests). */
    @FunctionalInterface
    public interface NativeLoader {
        /**
         * Loads the native libraries.
         *
         * @throws Exception when they cannot be loaded
         */
        void load() throws Exception;
    }

    /**
     * Spring constructor: the explainer is taken from the context when a bean exists, otherwise a default one is created.
     *
     * @param builder    the model builder (it owns the CP-SAT contributor registry)
     * @param explainer  provider of the infeasibility explainer (design 6.10 B)
     * @param properties the CP-SAT properties
     */
    @Autowired
    public CpSatSolverEngine(CpModelBuilder builder, ObjectProvider<CpSatExplainer> explainer,
                             CpSatProperties properties) {
        this(builder, explainer.getIfAvailable(CpSatExplainer::new), properties, Loader::loadNativeLibraries);
    }

    /**
     * Constructor with an explicit explainer.
     *
     * @param builder    the model builder
     * @param explainer  the infeasibility explainer
     * @param properties the CP-SAT properties
     */
    public CpSatSolverEngine(CpModelBuilder builder, CpSatExplainer explainer, CpSatProperties properties) {
        this(builder, explainer, properties, Loader::loadNativeLibraries);
    }

    /**
     * Constructor with a replaceable native loader (tests).
     *
     * @param builder      the model builder
     * @param explainer    the infeasibility explainer
     * @param properties   the CP-SAT properties
     * @param nativeLoader the native loading seam
     */
    public CpSatSolverEngine(CpModelBuilder builder, CpSatExplainer explainer, CpSatProperties properties,
                             NativeLoader nativeLoader) {
        this.builder = builder;
        this.runner = new SolveRunner(properties);
        this.explainer = explainer == null ? new CpSatExplainer() : explainer;
        this.nativeLoader = nativeLoader;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public EngineCapabilities capabilities() {
        CpSatContributorRegistry contributors = builder.contributors();
        Map<String, EnforcementSupport> constraints = new LinkedHashMap<>();
        for (String type : contributors.constraintTypes()) {
            contributors.constraint(type).ifPresent(contributor -> constraints.put(type, contributor.support()));
        }
        return new EngineCapabilities(NAME, EnumSet.allOf(EngineCapabilities.Feature.class), constraints,
                new TreeSet<>(contributors.objectiveTypes()), true);
    }

    /**
     * Probes the OR-Tools native library: loads it once on first call and caches the outcome. A missing or broken library
     * (for example a missing msvcp140.dll) makes only cp_sat jobs fail, not the whole service at startup. Intended for a
     * health indicator.
     *
     * @return true when the native library is loaded
     */
    public boolean isAvailable() {
        try {
            ensureNativeLoaded();
            return true;
        } catch (SolverUnavailableException exception) {
            return false;
        }
    }

    private void ensureNativeLoaded() {
        if (loaded) {
            return;
        }
        synchronized (loadLock) {
            if (loaded) {
                return;
            }
            if (loadFailure == null) {
                try {
                    nativeLoader.load();
                    loaded = true;
                    return;
                } catch (Throwable failure) {
                    loadFailure = failure;
                    log.error("OR-Tools native library could not be loaded; cp-sat solver is unavailable", failure);
                }
            }
            throw new SolverUnavailableException(name(), loadFailure);
        }
    }

    @Override
    public SolveResult solve(CompiledProblem problem, SolveContext context) {
        ensureNativeLoaded();
        long startNanos = context.startNanos();
        SolverOptions options = context.options() != null ? context.options() : problem.problem().options();

        BuiltModel built;
        try {
            built = builder.build(problem, context, BuildMode.SOLVE);
        } catch (SolveCancelledException stop) {
            return unknown(problem, startNanos, stop.getMessage(), null);
        }
        long buildMillis = elapsedMillis(startNanos);
        context.listener().onProgress(new SolveProgress(buildMillis, 0, null, null, null, 0, "BUILD"));

        SolveRunner.Outcome outcome = runner.run(built, context);
        if (outcome.status == CpSolverStatus.MODEL_INVALID) {
            throw new InvalidRequestException("INVALID_SOLVER_MODEL", "CP-SAT rejected the generated planning model");
        }
        if (outcome.status == CpSolverStatus.INFEASIBLE) {
            return infeasible(problem, context, options, built, outcome, startNanos, buildMillis);
        }
        if (!outcome.hasSolution()) {
            String message = outcome.cancelled ? SolveCancelledException.STOP_REQUESTED
                    : outcome.timeLimit ? SolveCancelledException.TIME_LIMIT : "No solution found within the time limit";
            return unknown(problem, startNanos, message, built, outcome, buildMillis);
        }
        boolean stopped = outcome.cancelled && outcome.status != CpSolverStatus.OPTIMAL;
        if (stopped && !Boolean.TRUE.equals(options.keepIncumbentOnStop())) {
            return unknown(problem, startNanos, SolveCancelledException.STOP_REQUESTED, built, outcome, buildMillis);
        }

        SolutionExtractor.Extracted extracted =
                SolutionExtractor.extract(built.context(), SolutionExtractor.Values.of(outcome.solver));
        IEnumSolverStatus.Types status;
        if (outcome.status == CpSolverStatus.OPTIMAL) {
            status = IEnumSolverStatus.Types.OPTIMAL;
        } else {
            status = stopped ? IEnumSolverStatus.Types.STOPPED_FEASIBLE : IEnumSolverStatus.Types.FEASIBLE;
        }
        PlanningSolution solution = new PlanningSolution(extracted.assignments(), extracted.unassigned(), List.of(),
                List.of(), List.of(), qualityScore(status, outcome.gap));

        List<String> relaxedIds = new ArrayList<>();
        List<Relaxation> relaxations = new ArrayList<>();
        for (CpSatEnforcerFactory.RelaxLiteral literal : built.relaxLiterals()) {
            if (!outcome.solver.booleanValue(literal.active())) {
                relaxedIds.add(literal.constraintId());
                IEnumEnforcement.Types original = enforcementOf(problem, literal.constraintIndex());
                relaxations.add(new Relaxation(literal.constraintId(), original == null ? null : original.name(),
                        "RELAXED", 0.0d, true, 0, List.of(), 0.0d));
            }
        }
        Map<String, Object> details = new LinkedHashMap<>();
        if (!relaxedIds.isEmpty()) {
            details.put("relaxedConstraintIds", relaxedIds);
        }
        if (outcome.deterministicBroken) {
            details.put("deterministicBroken", Boolean.TRUE);
        }
        Diagnostics diagnostics = new Diagnostics("Solve finished: " + status, List.of(), null, List.of(), details);
        return new SolveResult(status, solution, relaxations, diagnostics,
                metrics(problem, built, outcome, startNanos, buildMillis));
    }

    // -------------------------------------------------------------------------------------------- results

    private SolveResult infeasible(CompiledProblem problem, SolveContext context, SolverOptions options, BuiltModel built,
                                   SolveRunner.Outcome outcome, long startNanos, long buildMillis) {
        InfeasibilityCore core = null;
        if (Boolean.TRUE.equals(options.explainInfeasibility()) && !outcome.cancelled) {
            context.listener().onProgress(new SolveProgress(elapsedMillis(startNanos), outcome.solutions, null, null,
                    null, 0, "EXPLAIN"));
            try {
                core = explainer.explain(problem, context, builder);
            } catch (RuntimeException | LinkageError failure) {
                log.warn("Infeasibility explanation failed: {}", failure.toString());
            }
        }
        if (core == null) {
            core = fallbackCore(problem);
        }
        Diagnostics diagnostics = CoreItems.diagnostics("No feasible assignment satisfies the active hard constraints",
                core, List.of(), Map.of());
        return new SolveResult(IEnumSolverStatus.Types.INFEASIBLE, null, List.of(), diagnostics,
                metrics(problem, built, outcome, startNanos, buildMillis));
    }

    private static InfeasibilityCore fallbackCore(CompiledProblem problem) {
        List<CoreItem> items = new ArrayList<>();
        for (Constraint constraint : problem.problem().constraints()) {
            if (constraint.enforcement() != IEnumEnforcement.Types.SOFT) {
                items.add(CoreItems.item(CoreItems.CONSTRAINT, constraint.id(), CoreItems.CONSTRAINT_REASON,
                        "constraint '" + constraint.id() + "' (" + constraint.type() + ") may be part of the conflict"));
            }
        }
        return CoreItems.core(CoreItems.METHOD_NONE, false, items);
    }

    private SolveResult unknown(CompiledProblem problem, long startNanos, String message, BuiltModel built) {
        return unknown(problem, startNanos, message, built, null, elapsedMillis(startNanos));
    }

    private SolveResult unknown(CompiledProblem problem, long startNanos, String message, BuiltModel built,
                                SolveRunner.Outcome outcome, long buildMillis) {
        Diagnostics diagnostics = new Diagnostics(message, List.of(), null, List.of(), Map.of());
        Metrics metrics = outcome != null && built != null
                ? metrics(problem, built, outcome, startNanos, buildMillis)
                : new Metrics(elapsedMillis(startNanos), buildMillis, 0L, null, 0, structureStats(problem),
                new SearchStats(0L, 0L, 0, null, null, null, 0), List.of());
        return new SolveResult(IEnumSolverStatus.Types.UNKNOWN, null, List.of(), diagnostics, metrics);
    }

    private Metrics metrics(CompiledProblem problem, BuiltModel built, SolveRunner.Outcome outcome, long startNanos,
                            long buildMillis) {
        long duration = Math.max(elapsedMillis(startNanos), buildMillis + outcome.solveMillis);
        int workers = outcome.workers > 0 ? outcome.workers : 1;
        SearchStats search = new SearchStats(outcome.branches, outcome.conflicts, outcome.solutions, outcome.objective,
                outcome.bestBound, outcome.gap, outcome.tiersSolved);
        return new Metrics(duration, buildMillis, outcome.solveMillis, outcome.firstSolutionMillis, workers,
                built.stats(), search, outcome.tiers);
    }

    private static ModelStats structureStats(CompiledProblem problem) {
        int tasks = problem.index() == null ? problem.problem().tasks().size() : problem.index().taskCount();
        int resources = problem.index() == null ? problem.problem().resources().size() : problem.index().resourceCount();
        int candidates = problem.index() == null ? 0 : problem.index().candidateCount();
        return new ModelStats(tasks, resources, candidates, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    /** 1.0 when optimal, else {@code 1 / (1 + gap)} clamped to [0.01, 0.99], 0.5 when the gap is unknown. */
    private static double qualityScore(IEnumSolverStatus.Types status, Double gap) {
        if (status == IEnumSolverStatus.Types.OPTIMAL) {
            return 1.0d;
        }
        if (gap == null || gap.isNaN() || gap.isInfinite()) {
            return 0.5d;
        }
        return Math.min(0.99d, Math.max(0.01d, 1.0d / (1.0d + Math.max(0.0d, gap))));
    }

    private static IEnumEnforcement.Types enforcementOf(CompiledProblem problem, int constraintIndex) {
        for (CompiledConstraint compiled : problem.constraints()) {
            if (compiled.index() == constraintIndex) {
                return compiled.constraint().enforcement();
            }
        }
        return null;
    }

    private static long elapsedMillis(long startNanos) {
        return Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
    }
}
