package eu.isygoit.plan.engine.timefold;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.enums.IEnumSolverStatus;
import eu.isygoit.plan.domain.*;
import eu.isygoit.plan.engine.ConstraintRegistry;
import eu.isygoit.plan.engine.ObjectiveRegistry;
import eu.isygoit.plan.engine.SolverEngine;
import eu.isygoit.plan.engine.SolverCancellationToken;
import eu.isygoit.plan.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
public class TimefoldSolverEngine implements SolverEngine {
    private final ConstraintRegistry constraintRegistry;
    private final ObjectiveRegistry objectiveRegistry;

    public TimefoldSolverEngine(
            ConstraintRegistry constraintRegistry,
            ObjectiveRegistry objectiveRegistry) {
        this.constraintRegistry = constraintRegistry;
        this.objectiveRegistry = objectiveRegistry;
    }

    @Override
    public String name() {
        return "timefold";
    }

    @Override
    public SolveResult solve(PlanningProblem problem, SolverOptions options) {
        return solve(problem, options, new SolverCancellationToken());
    }

    @Override
    public SolveResult solve(
            PlanningProblem problem,
            SolverOptions options,
            SolverCancellationToken cancellationToken) {
        Instant started = Instant.now();
        if (problem.tasks().isEmpty()) {
            return new SolveResult(
                    IEnumSolverStatus.Types.OPTIMAL,
                    new PlanningSolution(List.of(), List.of(), List.of(), List.of(), List.of(), 1.0),
                    List.of(),
                    null,
                    new Metrics(0, 0, 0));
        }
        List<Constraint> relaxable = problem.constraints().stream()
                .filter(constraint -> constraint.enforcement() == IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION)
                .filter(constraint -> constraint.relaxation().authorized()
                        && constraint.relaxation().maxRelaxation() > 0.0)
                .filter(constraint -> options.relaxationLevels().isEmpty()
                        || options.relaxationLevels().contains(constraint.id()))
                .sorted(Comparator.comparingInt(constraint -> constraint.relaxation().priority()))
                .toList();
        Set<String> relaxedIds = new HashSet<>();

        for (int relaxationCount = 0; relaxationCount <= relaxable.size(); relaxationCount++) {
            if (cancellationToken.isCancellationRequested()) {
                return unknown(started, options, "Solver stop was requested");
            }
            long remainingSeconds = options.maxTimeSeconds()
                    - Duration.between(started, Instant.now()).toSeconds();
            if (remainingSeconds <= 0) {
                return unknown(started, options, "Solver time limit was reached");
            }
            SolveResult result = solveAttempt(
                    problem, options, relaxedIds, remainingSeconds, started, cancellationToken);
            if (result.status() != IEnumSolverStatus.Types.INFEASIBLE) {
                return withRelaxations(result, relaxable.subList(0, relaxationCount));
            }
            if (relaxationCount < relaxable.size()) {
                relaxedIds.add(relaxable.get(relaxationCount).id());
            }
        }
        return new SolveResult(IEnumSolverStatus.Types.INFEASIBLE, null, List.of(),
                new Diagnostics("No feasible assignment satisfies the active hard constraints",
                        problem.constraints().stream()
                                .filter(constraint -> constraint.enforcement() != IEnumEnforcement.Types.SOFT
                                        && !relaxedIds.contains(constraint.id()))
                                .map(Constraint::id).toList(), Map.of()),
                new Metrics(Duration.between(started, Instant.now()).toMillis(), 0, 1));
    }

    private SolveResult solveAttempt(
            PlanningProblem problem,
            SolverOptions options,
            Set<String> relaxedIds,
            long remainingSeconds,
            Instant started,
            SolverCancellationToken cancellationToken) {
        if (problem.resources().isEmpty() || problem.slots().isEmpty()) {
            List<String> requiredTaskIds = problem.tasks().stream()
                    .filter(Task::requiredTask)
                    .map(Task::id)
                    .toList();
            if (!requiredTaskIds.isEmpty()) {
                return infeasible(started, options, requiredTaskIds);
            }
            List<UnassignedTask> unassignedTasks = problem.tasks().stream()
                    .map(task -> new UnassignedTask(
                            task.id(),
                            "OPTIONAL_NOT_SELECTED",
                            "Optional task has no available resource or slot",
                            null))
                    .toList();
            return new SolveResult(
                    IEnumSolverStatus.Types.OPTIMAL,
                    new PlanningSolution(List.of(), unassignedTasks, List.of(), List.of(), List.of(), 1.0),
                    List.of(),
                    null,
                    new Metrics(Duration.between(started, Instant.now()).toMillis(), 0, 0));
        }

        List<TimefoldTaskAssignment> entities = new ArrayList<>();
        BigInteger maximumScoreMagnitude = BigInteger.ZERO;
        for (Task task : problem.tasks()) {
            Set<AssignmentKey> allowed = new HashSet<>();
            Map<AssignmentKey, Long> costs = new HashMap<>();
            Map<AssignmentKey, Integer> softPenalties = new HashMap<>();
            Map<ResourceSlotKey, Integer> capacities = new HashMap<>();
            BigInteger taskMaximumScoreMagnitude = BigInteger.ZERO;
            for (Resource resource : problem.resources()) {
                for (Slot slot : problem.slots()) {
                    AssignmentKey key = new AssignmentKey(task.id(), resource.id(), slot.id());
                    boolean candidate = matches(task.candidates().resourceSelectors(), resource.id())
                            && matches(task.candidates().slotSelectors(), slot.id())
                            && hasCapabilities(task, resource)
                            && !resource.unavailableSlots().contains(slot.id())
                            && constraintRegistry.allowsAssignment(problem, task, resource, slot, relaxedIds);
                    if (candidate) {
                        allowed.add(key);
                        long objectiveCost;
                        long softPenalty;
                        try {
                            objectiveCost = objectiveRegistry.assignmentCost(
                                    problem.objectives(), task, resource, slot);
                            softPenalty = constraintRegistry.assignmentPenalty(
                                    problem, task, resource, slot);
                        } catch (ArithmeticException exception) {
                            throw new InvalidRequestException("OBJECTIVE_SCORE_OUT_OF_RANGE",
                                    "Combined planning score exceeds the supported range");
                        }
                        if (softPenalty > Integer.MAX_VALUE) {
                            throw new InvalidRequestException("OBJECTIVE_SCORE_OUT_OF_RANGE",
                                    "Timefold's soft constraint penalty exceeds the supported range");
                        }
                        costs.put(key, objectiveCost);
                        softPenalties.put(key, Math.toIntExact(softPenalty));
                        BigInteger scoreMagnitude = BigInteger.valueOf(objectiveCost).abs()
                                .add(BigInteger.valueOf(softPenalty));
                        taskMaximumScoreMagnitude = taskMaximumScoreMagnitude.max(scoreMagnitude);
                    }
                    capacities.put(new ResourceSlotKey(resource.id(), slot.id()),
                            effectiveCapacity(problem, resource, slot, relaxedIds));
                }
            }
            if (task.requiredTask() && allowed.isEmpty()) {
                return infeasible(started, options, List.of(task.id()));
            }
            maximumScoreMagnitude = maximumScoreMagnitude.add(taskMaximumScoreMagnitude);
            if (maximumScoreMagnitude.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                throw new InvalidRequestException("OBJECTIVE_SCORE_OUT_OF_RANGE",
                        "Timefold's combined planning score exceeds the supported range");
            }
            entities.add(new TimefoldTaskAssignment(task, allowed, costs, softPenalties, capacities));
        }

        SolverConfig config = new SolverConfig()
                .withSolutionClass(TimefoldPlan.class)
                .withEntityClasses(TimefoldTaskAssignment.class)
                .withConstraintProviderClass(PlanConstraintProvider.class)
                .withTerminationSpentLimit(Duration.ofSeconds(remainingSeconds))
                .withRandomSeed(options.seed());
        Solver<TimefoldPlan> solver = SolverFactory.<TimefoldPlan>create(config).buildSolver();
        TimefoldPlan solved;
        try (SolverCancellationToken.Registration ignored =
                     cancellationToken.onCancellation(solver::terminateEarly)) {
            solved = solver.solve(new TimefoldPlan(
                    problem.resources(), problem.slots(), entities));
        }
        HardSoftScore score = solved.getScore();
        if (score == null) {
            return unknown(started, options, "Timefold returned no score");
        }
        if (score.getHardScore() < 0) {
            return infeasible(started, options, problem.constraints().stream()
                    .filter(constraint -> constraint.enforcement() != IEnumEnforcement.Types.SOFT
                            && !relaxedIds.contains(constraint.id()))
                    .map(Constraint::id).toList());
        }

        List<Assignment> assignments = solved.getAssignments().stream()
                .filter(entity -> entity.getResource() != null && entity.getSlot() != null)
                .map(entity -> new Assignment(entity.getTask().id(), entity.getSlot().id(),
                        List.of(new ResourceAssignment(entity.getResource().id(), "default")),
                        entity.getSlot().start(), entity.getSlot().end()))
                .toList();
        List<UnassignedTask> unassignedTasks = solved.getAssignments().stream()
                .filter(entity -> entity.getResource() == null && entity.getSlot() == null)
                .map(entity -> new UnassignedTask(
                        entity.getTask().id(),
                        "OPTIONAL_NOT_SELECTED",
                        "Optional task was not selected by the solver",
                        null))
                .toList();
        List<ConstraintViolation> softViolations = new ArrayList<>();
        for (TimefoldTaskAssignment entity : solved.getAssignments()) {
            if (entity.getResource() == null || entity.getSlot() == null) {
                continue;
            }
            constraintRegistry.violatedSoftConstraints(
                            problem, entity.getTask(), entity.getResource(), entity.getSlot())
                    .forEach(constraint -> softViolations.add(new ConstraintViolation(
                            constraint.id(),
                            entity.getTask().id(),
                            "Soft constraint was not satisfied",
                            constraint.weight())));
        }
        List<Relaxation> relaxations = relaxations(problem, relaxedIds);
        PlanningSolution solution = new PlanningSolution(
                assignments, unassignedTasks, List.of(), softViolations, List.of(),
                score.getSoftScore() == 0 ? 1.0 : 0.5);
        return new SolveResult(IEnumSolverStatus.Types.FEASIBLE, solution, relaxations, null,
                new Metrics(Duration.between(started, Instant.now()).toMillis(), 0, 1));
    }

    private int effectiveCapacity(
            PlanningProblem problem, Resource resource, Slot slot, Set<String> relaxedIds) {
        Object value = resource.attributes().get("capacity");
        int resourceCapacity = value == null ? Integer.MAX_VALUE : positiveInteger(value, resource.id());
        Integer constraintCapacity =
                constraintRegistry.resourceSlotCapacity(problem, resource, slot, relaxedIds);
        int configuredCapacity = constraintCapacity == null ? Integer.MAX_VALUE : constraintCapacity;
        int capacity = Math.min(resourceCapacity, configuredCapacity);
        return capacity == Integer.MAX_VALUE ? 1 : capacity;
    }

    private int positiveInteger(Object value, String resourceId) {
        if (!(value instanceof Number number)
                || number.doubleValue() != number.intValue()
                || number.intValue() < 1) {
            throw new InvalidRequestException("INVALID_RESOURCE_CAPACITY",
                    "Resource '" + resourceId + "' capacity must be a positive integer");
        }
        return number.intValue();
    }

    private boolean matches(List<String> selectors, String id) {
        return selectors.isEmpty() || selectors.stream().anyMatch(selector ->
                "ALL".equalsIgnoreCase(selector) || selector.equals(id));
    }

    private boolean hasCapabilities(Task task, Resource resource) {
        List<String> required = task.candidates().capabilityTags();
        if (required.isEmpty()) {
            return true;
        }
        Object value = resource.attributes().get("capabilities");
        return value instanceof List<?> capabilities && capabilities.containsAll(required);
    }

    private List<Relaxation> relaxations(PlanningProblem problem, Set<String> relaxedIds) {
        return problem.constraints().stream()
                .filter(constraint -> relaxedIds.contains(constraint.id()))
                .map(constraint -> new Relaxation(
                        constraint.id(), constraint.enforcement().name(), "RELAXED", 0.0, true))
                .toList();
    }

    private SolveResult withRelaxations(SolveResult result, List<Constraint> constraints) {
        if (constraints.isEmpty()) {
            return result;
        }
        List<Relaxation> relaxations = constraints.stream()
                .map(constraint -> new Relaxation(
                        constraint.id(), constraint.enforcement().name(), "RELAXED", 0.0, true))
                .toList();
        return new SolveResult(result.status(), result.solution(), relaxations,
                result.diagnostics(), result.metrics());
    }

    private SolveResult infeasible(Instant started, SolverOptions options, List<String> details) {
        return new SolveResult(IEnumSolverStatus.Types.INFEASIBLE, null, List.of(),
                new Diagnostics("No feasible assignment satisfies the active hard constraints", details, Map.of()),
                new Metrics(Duration.between(started, Instant.now()).toMillis(), 0, options.numWorkers()));
    }

    private SolveResult unknown(Instant started, SolverOptions options, String message) {
        return new SolveResult(IEnumSolverStatus.Types.UNKNOWN, null, List.of(),
                new Diagnostics(message, List.of(), Map.of()),
                new Metrics(Duration.between(started, Instant.now()).toMillis(), 0, options.numWorkers()));
    }
}
