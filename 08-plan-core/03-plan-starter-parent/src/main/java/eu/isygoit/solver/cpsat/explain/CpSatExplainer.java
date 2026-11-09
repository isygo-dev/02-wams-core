package eu.isygoit.solver.cpsat.explain;

import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.Literal;
import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.SolverOptions;
import eu.isygoit.dto.response.CoreItem;
import eu.isygoit.dto.response.InfeasibilityCore;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.CompiledProblem;
import eu.isygoit.solver.SolveContext;
import eu.isygoit.solver.SolveListener;
import eu.isygoit.solver.SolverCancellationToken;
import eu.isygoit.solver.cpsat.AssumptionRef;
import eu.isygoit.solver.cpsat.BuildMode;
import eu.isygoit.solver.cpsat.BuiltModel;
import eu.isygoit.solver.cpsat.CpModelBuilder;
import eu.isygoit.solver.diag.CoreItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase B of the infeasibility explanation (design 6.10): an IIS-style core from CP-SAT assumptions, best effort.
 * <p>
 * Called by the CP-SAT engine AFTER a solve returned INFEASIBLE and {@code explainInfeasibility} is on. It rebuilds the
 * model in {@link BuildMode#EXPLAIN} (soft constraints/objectives omitted, one assumption literal per non-soft
 * constraint, task, resource and precedence edge), solves it single-threaded under all assumptions and reads
 * {@code sufficientAssumptionsForInfeasibility()}; then a {@link DeletionFilter} minimises the core under the remaining
 * time budget. The pass is extra time on top of {@code maxTimeSeconds}, bounded by {@code explainTimeSeconds}: about 60%
 * for the first solve, the rest for shrinking (short limit per re-solve).
 * <p>
 * Graceful fallback: whenever the core cannot be computed (builder failure or limit, no assumptions, the explain model
 * is not infeasible under assumptions, an empty core, a time-out, natives missing) the result is
 * {@code method = NONE}, {@code minimal = false} with every non-SOFT catalogue constraint listed (the v1 behaviour, now
 * explicitly labelled). {@code explain} never throws for solver reasons.
 * <p>
 * Thread-safety: stateless; every call uses its own {@code CpSolver}. Units: seconds for budgets; the cancellation token
 * of the context stops the running solve.
 */
public final class CpSatExplainer {

    private static final Logger LOG = LoggerFactory.getLogger(CpSatExplainer.class);

    /** Maximum number of core items re-examined by the deletion filter (design: 30). */
    public static final int MAX_SHRINK_ITEMS = 30;

    /** Share of the explain budget given to the first (core-producing) solve. */
    private static final double FIRST_SOLVE_SHARE = 0.6;

    /** The pieces of a built EXPLAIN model the explainer needs. */
    public record ExplainModel(CpModel model, List<AssumptionRef> assumptions) {
        public ExplainModel {
            assumptions = assumptions == null ? List.of() : List.copyOf(assumptions);
        }
    }

    /** Builds the EXPLAIN model; the production implementation delegates to {@link CpModelBuilder}. */
    @FunctionalInterface
    public interface ModelFactory {
        /**
         * @param problem the compiled problem
         * @param context a fresh context whose budget is the explain budget (single worker)
         * @return the model and its assumption list
         */
        ExplainModel build(CompiledProblem problem, SolveContext context);
    }

    /**
     * Explains an infeasible problem.
     *
     * @param problem the compiled problem the engine found infeasible
     * @param context the solve context (options, cancellation token; the time budget comes from
     *                {@code explainTimeSeconds})
     * @param builder the CP-SAT model builder (WP3)
     * @return the core, never {@code null}
     */
    public InfeasibilityCore explain(CompiledProblem problem, SolveContext context, CpModelBuilder builder) {
        return explain(problem, context, (p, c) -> {
            BuiltModel built = builder.build(p, c, BuildMode.EXPLAIN);
            return new ExplainModel(built.model(), built.assumptions());
        });
    }

    /**
     * Same as {@link #explain(CompiledProblem, SolveContext, CpModelBuilder)} with an injectable model factory (tests).
     *
     * @param problem the compiled problem
     * @param context the solve context
     * @param factory builds the EXPLAIN model
     * @return the core, never {@code null}
     */
    public InfeasibilityCore explain(CompiledProblem problem, SolveContext context, ModelFactory factory) {
        long startNanos = System.nanoTime();
        SolverOptions base = context != null && context.options() != null
                ? context.options() : problem.problem().options();
        if (Boolean.FALSE.equals(base.explainInfeasibility())) {
            return fallback(problem);
        }
        int budgetSeconds = base.explainTimeSeconds() == null ? 1 : base.explainTimeSeconds();
        SolverCancellationToken token = context == null ? null : context.cancellation();
        try {
            SolverOptions explainOptions = new SolverOptions(base.engine(), budgetSeconds, 1, base.seed(), false,
                    base.relaxationLevels(), base.deterministic(), base.objectiveScale(), base.gapTolerance(),
                    base.lexicographicMode(), base.explainInfeasibility(), budgetSeconds,
                    base.keepIncumbentOnStop());
            SolveContext explainContext = new SolveContext(explainOptions, token, SolveListener.NOOP, startNanos);
            ExplainModel built = factory.build(problem, explainContext);
            if (built == null || built.model() == null || built.assumptions().isEmpty()) {
                LOG.info("Infeasibility explanation: no assumptions available, falling back");
                return fallback(problem);
            }
            return solveAndShrink(problem, built, explainOptions, token, startNanos, budgetSeconds);
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("Infeasibility explanation failed, falling back: {}", e.toString());
            return fallback(problem);
        }
    }

    // ------------------------------------------------------------------------------------------------ solve + shrink

    private InfeasibilityCore solveAndShrink(CompiledProblem problem, ExplainModel built, SolverOptions options,
                                             SolverCancellationToken token, long startNanos, int budgetSeconds) {
        List<AssumptionRef> refs = built.assumptions();
        Map<Integer, Integer> positionByLiteral = new HashMap<>();
        for (int i = 0; i < refs.size(); i++) {
            // OR-TOOLS-VERIFY: Literal.getIndex() is the proto literal index used by the solver core
            positionByLiteral.putIfAbsent(refs.get(i).literal().getIndex(), i);
        }
        CpSolver solver = new CpSolver();
        SolverCancellationToken.Registration registration = token == null
                ? null : token.onCancellation(solver::stopSearch);
        try {
            int[] all = new int[refs.size()];
            for (int i = 0; i < all.length; i++) {
                all[i] = i;
            }
            Outcome first = solveWith(solver, built.model(), refs, positionByLiteral, all, options,
                    Math.max(0.5, budgetSeconds * FIRST_SOLVE_SHARE));
            if (first.status != CpSolverStatus.INFEASIBLE || first.core.length == 0) {
                LOG.info("Infeasibility explanation: no core (status {}, core size {}), falling back",
                        first.status, first.core.length);
                return fallback(problem);
            }
            long deadline = startNanos + budgetSeconds * 1_000_000_000L;
            DeletionFilter filter = new DeletionFilter(MAX_SHRINK_ITEMS, deadline, System::nanoTime);
            DeletionFilter.Result shrunk = filter.shrink(first.core, subset -> {
                double remaining = (deadline - System.nanoTime()) / 1e9;
                double limit = Math.min(remaining, Math.max(0.5, budgetSeconds / 10.0));
                if (limit <= 0.0) {
                    return DeletionFilter.Verdict.unknown();
                }
                Outcome outcome = solveWith(solver, built.model(), refs, positionByLiteral, subset, options, limit);
                if (outcome.status == CpSolverStatus.INFEASIBLE) {
                    return DeletionFilter.Verdict.infeasible(outcome.core);
                }
                if (outcome.status == CpSolverStatus.OPTIMAL || outcome.status == CpSolverStatus.FEASIBLE) {
                    return DeletionFilter.Verdict.satisfiable();
                }
                return DeletionFilter.Verdict.unknown();
            });
            int[] finalCore = shrunk.core().length == 0 ? first.core : shrunk.core();
            boolean minimal = shrunk.core().length != 0 && shrunk.minimal();
            String method = shrunk.oracleCalls() > 0 ? CoreItems.METHOD_ASSUMPTIONS_SHRUNK
                    : CoreItems.METHOD_ASSUMPTIONS;
            return CoreItems.core(method, minimal, toItems(refs, finalCore));
        } finally {
            if (registration != null) {
                registration.close();
            }
        }
    }

    private static final class Outcome {
        final CpSolverStatus status;
        final int[] core;

        Outcome(CpSolverStatus status, int[] core) {
            this.status = status;
            this.core = core;
        }
    }

    /** Solves the model under the assumptions at the given positions; the core is returned as positions. */
    private static Outcome solveWith(CpSolver solver, CpModel model, List<AssumptionRef> refs,
                                     Map<Integer, Integer> positionByLiteral, int[] subset, SolverOptions options,
                                     double seconds) {
        model.clearAssumptions(); // OR-TOOLS-VERIFY: CpModel.clearAssumptions() exists in 9.15 Java
        if (subset.length > 0) {
            Literal[] literals = new Literal[subset.length];
            for (int i = 0; i < subset.length; i++) {
                literals[i] = refs.get(subset[i]).literal();
            }
            model.addAssumptions(literals); // OR-TOOLS-VERIFY: CpModel.addAssumptions(Literal[])
        }
        solver.getParameters().setMaxTimeInSeconds(seconds);
        solver.getParameters().setNumWorkers(1); // OR-TOOLS-VERIFY: SatParameters.Builder.setNumWorkers (was setNumSearchWorkers)
        solver.getParameters().setRandomSeed((int) (long) options.seed());
        solver.getParameters().setLogSearchProgress(false);
        CpSolverStatus status = solver.solve(model);
        if (status != CpSolverStatus.INFEASIBLE) {
            return new Outcome(status, new int[0]);
        }
        // OR-TOOLS-VERIFY: CpSolver.sufficientAssumptionsForInfeasibility() returns the literal indices as int[]
        List<Integer> raw = solver.sufficientAssumptionsForInfeasibility();
        List<Integer> positions = new ArrayList<>(raw.size());
        for (int literalIndex : raw) {
            Integer position = positionByLiteral.get(literalIndex);
            if (position != null && !positions.contains(position)) {
                positions.add(position);
            }
        }
        int[] core = new int[positions.size()];
        for (int i = 0; i < core.length; i++) {
            core[i] = positions.get(i);
        }
        return new Outcome(status, core);
    }

    // ------------------------------------------------------------------------------------------------ items

    private static List<CoreItem> toItems(List<AssumptionRef> refs, int[] core) {
        List<CoreItem> items = new ArrayList<>(core.length);
        for (int position : core) {
            AssumptionRef ref = refs.get(position);
            String kind = ref.kind() == null ? CoreItems.CONSTRAINT : ref.kind();
            String reason = ref.reasonCode() != null ? ref.reasonCode() : defaultReason(kind);
            items.add(CoreItems.item(kind, ref.id(), reason,
                    kind + " '" + ref.id() + "' is part of an infeasible set of requirements"));
        }
        return items;
    }

    private static String defaultReason(String kind) {
        return switch (kind) {
            case CoreItems.TASK -> CoreItems.WINDOW_EMPTY;
            case CoreItems.RESOURCE -> CoreItems.CALENDAR;
            case CoreItems.EDGE -> CoreItems.PRECEDENCE;
            default -> CoreItems.CONSTRAINT_REASON;
        };
    }

    /**
     * The labelled v1 fallback: method {@code NONE}, not minimal, every non-SOFT catalogue constraint listed.
     *
     * @param problem the compiled problem
     * @return the fallback core
     */
    static InfeasibilityCore fallback(CompiledProblem problem) {
        List<CoreItem> items = new ArrayList<>();
        for (Constraint constraint : problem.problem().constraints()) {
            if (constraint.enforcement() != IEnumEnforcement.Types.SOFT) {
                items.add(CoreItems.item(CoreItems.CONSTRAINT, constraint.id(), CoreItems.CONSTRAINT_REASON,
                        "constraint '" + constraint.id() + "' (" + constraint.type()
                                + ") may be part of the conflict; the exact core could not be computed"));
            }
        }
        return CoreItems.core(CoreItems.METHOD_NONE, false, items);
    }
}
