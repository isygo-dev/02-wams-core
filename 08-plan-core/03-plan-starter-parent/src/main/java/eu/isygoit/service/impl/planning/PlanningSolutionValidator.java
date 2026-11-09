package eu.isygoit.service.impl.planning;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.InitialAssignment;
import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.ResourceRequirement;
import eu.isygoit.dto.request.Task;
import eu.isygoit.dto.response.Assignment;
import eu.isygoit.dto.response.ConstraintViolation;
import eu.isygoit.dto.response.Diagnostics;
import eu.isygoit.dto.response.DiagnosticIssue;
import eu.isygoit.dto.response.PlanningSolution;
import eu.isygoit.dto.response.Relaxation;
import eu.isygoit.dto.response.ResourceAssignment;
import eu.isygoit.dto.response.SolveResult;
import eu.isygoit.dto.response.UnassignedTask;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.enums.IEnumSolverStatus;
import eu.isygoit.exception.InvalidSolverResultException;
import eu.isygoit.exception.ObjectiveRangeException;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.CompiledObjective;
import eu.isygoit.solver.CompiledProblem;
import eu.isygoit.solver.check.CheckContextImpl;
import eu.isygoit.solver.check.ObjectiveAggregator;
import eu.isygoit.solver.check.RelaxationReporter;
import eu.isygoit.solver.check.SolutionViewImpl;
import eu.isygoit.solver.check.StructuralSolutionChecker;
import eu.isygoit.solver.check.UnselectedReasoner;
import eu.isygoit.solver.check.ViolationCollector;
import eu.isygoit.solver.registry.ConstraintFamilyRegistry;
import eu.isygoit.solver.registry.ObjectiveFamilyRegistry;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.CheckContext;
import eu.isygoit.solver.spi.ConstraintFamily;
import eu.isygoit.solver.spi.ObjectiveFamily;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Independent post-solve validator and enricher (design 8.1-8.4). Replaces the Phase 0-2 slot/capacity re-check.
 * <p>
 * After an engine returns a result with a solution, {@link #validateAndEnrich(CompiledProblem, SolveResult)}:
 * <ol>
 *   <li>checks the claimed relaxations are authorised (enforcement {@code RELAXABLE_WITH_AUTHORIZATION}, policy
 *       authorised with {@code maxRelaxation > 0}, allowed by {@code relaxationLevels});</li>
 *   <li>builds the int-indexed {@code SolutionView} once and runs the {@link StructuralSolutionChecker} (design 4.2);</li>
 *   <li>runs the {@code checker()} of EVERY constraint family through the registries: HARD violations (and violations of
 *       a RELAXABLE constraint the engine did not drop) invalidate the result, violations of a dropped RELAXABLE
 *       constraint become real {@link Relaxation}s ({@link RelaxationReporter}) and {@code hardViolations}, SOFT
 *       violations become {@code softViolations} (impact = weight x measure) and add to their tier;</li>
 *   <li>re-evaluates every objective ({@link ObjectiveAggregator}) and compares each tier {@code >= -1} with
 *       {@code Metrics.tiers} (exact, zero tolerance): the safety net that keeps every contributor honest;</li>
 *   <li>fills {@code unassignedTasks} reasons ({@link UnselectedReasoner}), {@code qualityDetails}, {@code qualityScore}
 *       and sorts everything deterministically (8.3).</li>
 * </ol>
 * Any inconsistency throws {@link InvalidSolverResultException}; the message starts with a stable code
 * ({@code SOLUTION_INVALID_*} or {@code TIER_VALUE_MISMATCH}) followed by a colon, then names the family/task.
 * <p>
 * A result without a solution and with status INFEASIBLE or UNKNOWN is returned unchanged. Stateless and thread-safe
 * (all state is per call); registries are immutable.
 */
@Component
public class PlanningSolutionValidator {

    /** Code prefix of structural and family violations. */
    public static final String CODE_PREFIX = "SOLUTION_INVALID_";
    /** Code of a tier whose recomputed value differs from the engine's. */
    public static final String TIER_VALUE_MISMATCH = "TIER_VALUE_MISMATCH";

    private final ConstraintFamilyRegistry constraintFamilies;
    private final ObjectiveFamilyRegistry objectiveFamilies;
    private final RelaxationReporter relaxationReporter = new RelaxationReporter();
    private final ObjectiveAggregator objectiveAggregator = new ObjectiveAggregator();

    /**
     * @param constraintFamilies registry used when a compiled constraint carries no family reference
     * @param objectiveFamilies  registry used when a compiled objective carries no family reference
     */
    public PlanningSolutionValidator(ConstraintFamilyRegistry constraintFamilies,
                                     ObjectiveFamilyRegistry objectiveFamilies) {
        this.constraintFamilies = constraintFamilies;
        this.objectiveFamilies = objectiveFamilies;
    }

    /**
     * Validates and enriches an engine result.
     *
     * @param compiled the compiled problem the engine solved
     * @param result   the engine result
     * @return the enriched, deterministically ordered result
     * @throws InvalidSolverResultException when the engine produced an invalid plan or wrong tier values
     */
    public SolveResult validateAndEnrich(CompiledProblem compiled, SolveResult result) {
        return validateAndEnrich(compiled.problem(), compiled.index(), compiled.constraints(),
                compiled.objectives(), result);
    }

    /**
     * Same as {@link #validateAndEnrich(CompiledProblem, SolveResult)} on the individual parts (lets tests and callers
     * without a full {@code CandidateIndex} provide any {@link CandidateIndexView}).
     *
     * @param problem     the normalized problem
     * @param index       full index view (per-task data is needed for the unselected reasons)
     * @param constraints compiled constraints, request order (all of them, including preprocessed ones)
     * @param objectives  compiled objectives, request order
     * @param result      the engine result
     * @return the enriched result
     */
    public SolveResult validateAndEnrich(PlanningProblem problem, CandidateIndexView index,
                                         List<CompiledConstraint> constraints, List<CompiledObjective> objectives,
                                         SolveResult result) {
        if (result.solution() == null) {
            IEnumSolverStatus.Types status = result.status();
            if (status == IEnumSolverStatus.Types.OPTIMAL || status == IEnumSolverStatus.Types.FEASIBLE
                    || status == IEnumSolverStatus.Types.STOPPED_FEASIBLE) {
                throw invalid(CODE_PREFIX + "MISSING_SOLUTION",
                        "A feasible solver result must include a solution (status " + status + ")");
            }
            return result;
        }
        Map<String, Constraint> constraintsById = new HashMap<>();
        for (Constraint constraint : problem.constraints()) {
            constraintsById.put(constraint.id(), constraint);
        }
        Set<String> relaxedIds = checkRelaxations(problem, constraintsById, result.relaxations());

        PlanningSolution solution = result.solution();
        SolutionViewImpl view;
        try {
            view = SolutionViewImpl.build(index, solution.assignments());
        } catch (SolutionViewImpl.MalformedSolutionException e) {
            throw invalid(e.code(), e.getMessage());
        }
        checkUnassignedEntries(index, problem, view, solution.unassignedTasks());

        StructuralSolutionChecker structural = new StructuralSolutionChecker(problem, index);
        List<StructuralSolutionChecker.Violation> broken = new ArrayList<>(structural.checkRecords(
                solution.assignments()));
        broken.addAll(structural.check(view));
        if (!broken.isEmpty()) {
            StructuralSolutionChecker.Violation first = broken.get(0);
            throw invalid(first.code(), first.message()
                    + (broken.size() > 1 ? " (+" + (broken.size() - 1) + " more violation(s))" : ""));
        }

        CheckContext ctx = new CheckContextImpl(problem, index, view);
        ObjectiveAggregator.Aggregate aggregate = evaluateObjectives(ctx, objectives);

        List<ConstraintViolation> hardViolations = new ArrayList<>();
        List<ConstraintViolation> softViolations = new ArrayList<>();
        List<Relaxation> relaxations = new ArrayList<>();
        Set<String> reportedRelaxations = new HashSet<>();
        for (CompiledConstraint compiledConstraint : constraints) {
            Constraint constraint = compiledConstraint.constraint();
            ConstraintFamily family = compiledConstraint.family() != null
                    ? compiledConstraint.family() : constraintFamilies.get(constraint.type());
            ViolationCollector collector = new ViolationCollector();
            family.checker().check(ctx, constraint, compiledConstraint.params(), collector);
            if (collector.isEmpty()) {
                continue;
            }
            switch (constraint.enforcement()) {
                case SOFT -> {
                    for (ViolationCollector.Entry entry : collector.entries()) {
                        softViolations.add(new ConstraintViolation(constraint.id(), entry.taskId(),
                                entry.resourceId(), entry.code(), entry.message(),
                                (double) constraint.weight() * (double) entry.measure()));
                    }
                    try {
                        aggregate.addSoftPenalty(constraint.tier(), constraint.weight(), collector.totalMeasure());
                    } catch (ObjectiveRangeException e) {
                        throw invalid(CODE_PREFIX + "OBJECTIVE_RANGE",
                                "soft penalties of constraint '" + constraint.id() + "' overflow: " + e.getMessage());
                    }
                }
                case RELAXABLE_WITH_AUTHORIZATION -> {
                    if (!relaxedIds.contains(constraint.id())) {
                        throw hardViolation(constraint, collector);
                    }
                    relaxationReporter.report(constraint, collector).ifPresent(r -> {
                        if (reportedRelaxations.add(r.constraintId())) {
                            relaxations.add(r);
                        }
                    });
                    hardViolations.addAll(relaxationReporter.hardViolations(constraint, collector));
                }
                default -> throw hardViolation(constraint, collector);
            }
        }

        List<ObjectiveAggregator.Mismatch> mismatches = aggregate.compareWith(result.metrics());
        if (!mismatches.isEmpty()) {
            ObjectiveAggregator.Mismatch m = mismatches.get(0);
            throw invalid(TIER_VALUE_MISMATCH, "tier " + m.tier() + ": the validator recomputed " + m.computed()
                    + " but the engine reported " + m.reported() + " (a contributor and its checker disagree)");
        }

        List<UnassignedTask> unassigned = new UnselectedReasoner(problem, index).reason(view);
        List<Assignment> assignments = sortedAssignments(problem, index, solution.assignments());
        softViolations.sort(violationOrder());
        hardViolations.sort(violationOrder());
        relaxations.sort(Comparator.comparing(Relaxation::constraintId));

        PlanningSolution enriched = new PlanningSolution(assignments, unassigned, hardViolations, softViolations,
                aggregate.scores(), qualityScore(result));
        Diagnostics diagnostics = withWarnings(result.diagnostics(), hintWarnings(problem, index));
        return new SolveResult(result.status(), enriched, relaxations, diagnostics, result.metrics());
    }

    // ------------------------------------------------------------------------------------------------ steps

    private Set<String> checkRelaxations(PlanningProblem problem, Map<String, Constraint> byId,
                                         List<Relaxation> relaxations) {
        Set<String> ids = new HashSet<>();
        List<String> levels = problem.options().relaxationLevels();
        for (Relaxation relaxation : relaxations) {
            Constraint constraint = byId.get(relaxation.constraintId());
            if (constraint == null) {
                throw invalid(CODE_PREFIX + "RELAXATION",
                        "Solver returned an unknown relaxed constraint '" + relaxation.constraintId() + "'");
            }
            if (constraint.enforcement() != IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION
                    || constraint.relaxation() == null
                    || !constraint.relaxation().authorized()
                    || constraint.relaxation().maxRelaxation() <= 0.0
                    || (!levels.isEmpty() && !levels.contains(constraint.id()))) {
                throw invalid(CODE_PREFIX + "RELAXATION",
                        "Solver attempted to relax constraint '" + constraint.id()
                                + "' which is not authorized for relaxation");
            }
            if (!ids.add(constraint.id())) {
                throw invalid(CODE_PREFIX + "RELAXATION",
                        "Solver reported constraint '" + constraint.id() + "' as relaxed more than once");
            }
        }
        return ids;
    }

    private void checkUnassignedEntries(CandidateIndexView index, PlanningProblem problem, SolutionViewImpl view,
                                        List<UnassignedTask> entries) {
        Map<String, Task> tasks = new HashMap<>();
        for (Task task : problem.tasks()) {
            tasks.put(task.id(), task);
        }
        Set<String> seen = new HashSet<>();
        for (UnassignedTask entry : entries) {
            Task task = tasks.get(entry.taskId());
            int t = index.taskIdx(entry.taskId());
            if (task == null || t < 0 || task.requiredTask() || view.selected(t) || !seen.add(entry.taskId())) {
                throw invalid(CODE_PREFIX + "UNASSIGNED",
                        "Solver returned an invalid unassigned-task entry for '" + entry.taskId() + "'");
            }
        }
    }

    private ObjectiveAggregator.Aggregate evaluateObjectives(CheckContext ctx, List<CompiledObjective> objectives) {
        List<CompiledObjective> resolved = new ArrayList<>(objectives.size());
        for (CompiledObjective objective : objectives) {
            if (objective.family() != null) {
                resolved.add(objective);
            } else {
                Objective def = objective.objective();
                ObjectiveFamily family = objectiveFamilies.get(def.type());
                resolved.add(new CompiledObjective(objective.index(), def, family, objective.params(),
                        objective.tier(), objective.scale()));
            }
        }
        try {
            return objectiveAggregator.aggregate(ctx, resolved);
        } catch (ObjectiveRangeException e) {
            throw invalid(CODE_PREFIX + "OBJECTIVE_RANGE", "objective evaluation overflows: " + e.getMessage());
        }
    }

    private static InvalidSolverResultException hardViolation(Constraint constraint, ViolationCollector collector) {
        ViolationCollector.Entry first = collector.entries().get(0);
        return invalid(CODE_PREFIX + "CONSTRAINT", "constraint '" + constraint.id() + "' (" + constraint.type()
                + ") is violated"
                + (first.taskId() == null ? "" : " by task '" + first.taskId() + "'")
                + (first.resourceId() == null ? "" : " on resource '" + first.resourceId() + "'")
                + ": " + first.message()
                + (collector.count() > 1 ? " (+" + (collector.count() - 1) + " more)" : ""));
    }

    private static List<Assignment> sortedAssignments(PlanningProblem problem, CandidateIndexView index,
                                                      List<Assignment> assignments) {
        Map<String, Task> tasks = new HashMap<>();
        for (Task task : problem.tasks()) {
            tasks.put(task.id(), task);
        }
        List<Assignment> out = new ArrayList<>(assignments.size());
        for (Assignment assignment : assignments) {
            Task task = tasks.get(assignment.taskId());
            Map<String, Integer> roleOrder = new HashMap<>();
            if (task != null) {
                int i = 0;
                for (ResourceRequirement requirement : task.effectiveRequirements()) {
                    roleOrder.putIfAbsent(requirement.role(), i++);
                }
            }
            List<ResourceAssignment> resources = new ArrayList<>(assignment.resources());
            resources.sort(Comparator
                    .comparingInt((ResourceAssignment ra) -> roleOrder.getOrDefault(ra.role(), Integer.MAX_VALUE))
                    .thenComparing(ResourceAssignment::resourceId));
            out.add(new Assignment(assignment.taskId(), assignment.optional(), assignment.startTick(),
                    assignment.endTick(), assignment.start(), assignment.end(), assignment.slotId(), resources));
        }
        out.sort(Comparator.comparingInt(Assignment::startTick).thenComparing(Assignment::taskId));
        return out;
    }

    private static Comparator<ConstraintViolation> violationOrder() {
        return Comparator.comparing(ConstraintViolation::constraintId)
                .thenComparing(ConstraintViolation::taskId, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(ConstraintViolation::resourceId, Comparator.nullsFirst(Comparator.naturalOrder()));
    }

    /** 1.0 when OPTIMAL, else {@code 1/(1+gap)} clamped to [0.01, 0.99]; 0.5 when the gap is unknown. */
    static double qualityScore(SolveResult result) {
        if (result.status() == IEnumSolverStatus.Types.OPTIMAL) {
            return 1.0;
        }
        Double gap = result.metrics() == null || result.metrics().search() == null
                ? null : result.metrics().search().gap();
        if (gap == null || !Double.isFinite(gap) || gap < 0.0) {
            return 0.5;
        }
        return Math.max(0.01, Math.min(0.99, 1.0 / (1.0 + gap)));
    }

    /** {@code HINT_OUT_OF_DOMAIN} warnings for non-locked hints outside the pruned start domain (design 6.11). */
    private static List<DiagnosticIssue> hintWarnings(PlanningProblem problem, CandidateIndexView index) {
        List<DiagnosticIssue> warnings = new ArrayList<>();
        List<InitialAssignment> hints = problem.initialSolution().assignments();
        try {
            for (int i = 0; i < hints.size(); i++) {
                InitialAssignment hint = hints.get(i);
                int t = index.taskIdx(hint.taskId());
                if (hint.locked() || hint.start() == null || t < 0) {
                    continue;
                }
                int tick = problem.horizon().ceilTick(hint.start());
                if (tick < index.estStart(t) || tick > index.lstStart(t)) {
                    warnings.add(new DiagnosticIssue("HINT_OUT_OF_DOMAIN", "WARNING",
                            "initialSolution.assignments[" + i + "]", "the start hint of task '" + hint.taskId()
                            + "' lies outside the feasible start domain and is ignored by the solver"));
                }
            }
        } catch (UnsupportedOperationException lightView) {
            // a light view has no domains: nothing to report
        }
        return warnings;
    }

    private static Diagnostics withWarnings(Diagnostics base, List<DiagnosticIssue> extra) {
        if (extra.isEmpty()) {
            return base;
        }
        if (base == null) {
            return new Diagnostics(null, null, null, extra, new LinkedHashMap<>());
        }
        List<DiagnosticIssue> merged = new ArrayList<>(base.warnings());
        merged.addAll(extra);
        return new Diagnostics(base.message(), base.infeasibilityCore(), base.core(), merged, base.details());
    }

    /**
     * Builds the exception carrying the stable validator code ({@code SOLUTION_INVALID_*}, {@code TIER_VALUE_MISMATCH}).
     */
    private static InvalidSolverResultException invalid(String code, String message) {
        return new InvalidSolverResultException(code, code + ": " + message);
    }
}
