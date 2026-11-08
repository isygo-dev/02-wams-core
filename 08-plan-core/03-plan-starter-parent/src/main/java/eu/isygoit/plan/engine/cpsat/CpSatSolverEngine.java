package eu.isygoit.plan.engine.cpsat;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.enums.IEnumSolverStatus;
import eu.isygoit.plan.domain.*;
import eu.isygoit.plan.domain.Constraint;
import eu.isygoit.plan.engine.ConstraintRegistry;
import eu.isygoit.plan.engine.ObjectiveRegistry;
import eu.isygoit.plan.engine.SolverEngine;
import eu.isygoit.plan.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
public class CpSatSolverEngine implements SolverEngine {
    private final ConstraintRegistry constraintRegistry;
    private final ObjectiveRegistry objectiveRegistry;

    public CpSatSolverEngine(
            ConstraintRegistry constraintRegistry,
            ObjectiveRegistry objectiveRegistry) {
        this.constraintRegistry = constraintRegistry;
        this.objectiveRegistry = objectiveRegistry;
        Loader.loadNativeLibraries();
    }

    @Override
    public String name() {
        return "cp_sat";
    }

    @Override
    public SolveResult solve(PlanningProblem problem, SolverOptions options) {
        Instant started = Instant.now();
        List<Constraint> relaxable = problem.constraints().stream()
                .filter(constraint -> constraint.enforcement() == IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION)
                .filter(constraint -> constraint.relaxation().authorized()
                        && constraint.relaxation().maxRelaxation() > 0.0)
                .filter(constraint -> options.relaxationLevels().isEmpty()
                        || options.relaxationLevels().contains(constraint.id()))
                .sorted(Comparator.comparingInt(constraint -> constraint.relaxation().priority()))
                .toList();

        Set<String> relaxedIds = new HashSet<>();
        SolveResult lastResult = null;
        for (int relaxationCount = 0; relaxationCount <= relaxable.size(); relaxationCount++) {
            long remainingSeconds = options.maxTimeSeconds()
                    - Duration.between(started, Instant.now()).toSeconds();
            if (remainingSeconds <= 0) {
                return unknownResult(started, options, relaxedIds, "Solver time limit was reached");
            }
            lastResult = solveAttempt(problem, options, relaxedIds, remainingSeconds, started);
            if (lastResult.status() != IEnumSolverStatus.Types.INFEASIBLE) {
                return withRelaxations(lastResult, relaxable.subList(0, relaxationCount));
            }
            if (relaxationCount < relaxable.size()) {
                relaxedIds.add(relaxable.get(relaxationCount).id());
            }
        }
        return lastResult;
    }

    private SolveResult solveAttempt(
            PlanningProblem problem,
            SolverOptions options,
            Set<String> relaxedIds,
            long remainingSeconds,
            Instant started) {
        CpModel model = new CpModel();
        List<Choice> choices = new ArrayList<>();
        Map<String, List<BoolVar>> variablesByTask = new HashMap<>();
        Map<String, List<BoolVar>> variablesByResourceSlot = new HashMap<>();
        LinearExprBuilder objective = LinearExpr.newBuilder();
        BigInteger maximumObjectiveMagnitude = BigInteger.ZERO;

        for (Task task : problem.tasks()) {
            BigInteger taskMaximumObjectiveMagnitude = BigInteger.ZERO;
            for (Resource resource : problem.resources()) {
                if (!isResourceCandidate(task, resource) || !hasCapabilities(task, resource)) {
                    continue;
                }
                for (Slot slot : problem.slots()) {
                    if (!isSlotCandidate(task, slot) || resource.unavailableSlots().contains(slot.id())
                            || !constraintRegistry.allowsAssignment(
                            problem, task, resource, slot, relaxedIds)) {
                        continue;
                    }
                    BoolVar variable = model.newBoolVar(task.id() + "|" + resource.id() + "|" + slot.id());
                    Choice choice = new Choice(variable, task, resource, slot);
                    choices.add(choice);
                    variablesByTask.computeIfAbsent(task.id(), ignored -> new ArrayList<>()).add(variable);
                    variablesByResourceSlot.computeIfAbsent(resource.id() + "|" + slot.id(),
                            ignored -> new ArrayList<>()).add(variable);
                    long cost = objectiveRegistry.assignmentCost(
                            problem.objectives(), task, resource, slot);
                    try {
                        cost = Math.addExact(cost,
                                constraintRegistry.assignmentPenalty(problem, task, resource, slot));
                    } catch (ArithmeticException exception) {
                        throw new InvalidRequestException("OBJECTIVE_SCORE_OUT_OF_RANGE",
                                "Combined planning score exceeds the supported range");
                    }
                    objective.addTerm(variable, cost);
                    taskMaximumObjectiveMagnitude =
                            taskMaximumObjectiveMagnitude.max(BigInteger.valueOf(cost).abs());
                }
            }
            maximumObjectiveMagnitude = maximumObjectiveMagnitude.add(taskMaximumObjectiveMagnitude);
            if (maximumObjectiveMagnitude.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
                throw new InvalidRequestException("OBJECTIVE_SCORE_OUT_OF_RANGE",
                        "Combined planning score exceeds the CP-SAT supported range");
            }
        }

        List<String> tasksWithoutCandidates = new ArrayList<>();
        for (Task task : problem.tasks()) {
            List<BoolVar> taskVariables = variablesByTask.getOrDefault(task.id(), List.of());
            if (task.requiredTask() && taskVariables.isEmpty()) {
                tasksWithoutCandidates.add(task.id());
            } else if (task.requiredTask()) {
                model.addExactlyOne(taskVariables.toArray(BoolVar[]::new));
            } else if (!taskVariables.isEmpty()) {
                model.addAtMostOne(taskVariables.toArray(BoolVar[]::new));
            }
        }
        if (!tasksWithoutCandidates.isEmpty()) {
            return infeasibleResult(started, options, tasksWithoutCandidates);
        }

        for (Resource resource : problem.resources()) {
            for (Slot slot : problem.slots()) {
                List<BoolVar> variables = variablesByResourceSlot.getOrDefault(
                        resource.id() + "|" + slot.id(), List.of());
                if (!variables.isEmpty()) {
                    int capacity = effectiveCapacity(problem, resource, slot, relaxedIds);
                    model.addLessOrEqual(LinearExpr.sum(variables.toArray(LinearArgument[]::new)), capacity);
                }
            }
        }
        if (!problem.objectives().isEmpty()
                || problem.constraints().stream().anyMatch(c -> c.enforcement() == IEnumEnforcement.Types.SOFT)) {
            model.minimize(objective);
        }

        CpSolver solver = new CpSolver();
        solver.getParameters()
                .setMaxTimeInSeconds(remainingSeconds)
                .setNumSearchWorkers(options.numWorkers())
                .setRandomSeed((int) options.seed())
                .setLogSearchProgress(options.logSearch());
        CpSolverStatus solverStatus = solver.solve(model);
        if (solverStatus == CpSolverStatus.INFEASIBLE) {
            return infeasibleResult(started, options,
                    problem.constraints().stream()
                            .filter(constraint -> constraint.enforcement() != IEnumEnforcement.Types.SOFT
                                    && !relaxedIds.contains(constraint.id()))
                            .map(Constraint::id)
                            .toList());
        }
        if (solverStatus == CpSolverStatus.MODEL_INVALID) {
            throw new InvalidRequestException("INVALID_SOLVER_MODEL",
                    "CP-SAT rejected the generated planning model");
        }
        if (solverStatus != CpSolverStatus.OPTIMAL && solverStatus != CpSolverStatus.FEASIBLE) {
            return unknownResult(started, options, relaxedIds, solverStatus.name());
        }

        List<Assignment> assignments = new ArrayList<>();
        Set<String> assignedTaskIds = new HashSet<>();
        List<ConstraintViolation> softViolations = new ArrayList<>();
        for (Choice choice : choices) {
            if (solver.booleanValue(choice.variable())) {
                assignedTaskIds.add(choice.task().id());
                assignments.add(new Assignment(
                        choice.task().id(),
                        choice.slot().id(),
                        List.of(new ResourceAssignment(choice.resource().id(), "default")),
                        choice.slot().start(),
                        choice.slot().end()));
                constraintRegistry.violatedSoftConstraints(
                                problem, choice.task(), choice.resource(), choice.slot())
                        .forEach(constraint -> softViolations.add(new ConstraintViolation(
                                constraint.id(),
                                choice.task().id(),
                                "Soft constraint was not satisfied",
                                constraint.weight())));
            }
        }
        List<UnassignedTask> unassignedTasks = problem.tasks().stream()
                .filter(task -> !task.requiredTask() && !assignedTaskIds.contains(task.id()))
                .map(task -> new UnassignedTask(
                        task.id(),
                        "OPTIONAL_NOT_SELECTED",
                        "Optional task was not selected by the solver",
                        null))
                .toList();
        PlanningSolution solution = new PlanningSolution(
                assignments, unassignedTasks, List.of(), softViolations, List.of(),
                solverStatus == CpSolverStatus.OPTIMAL ? 1.0 : 0.5);
        return new SolveResult(
                solverStatus == CpSolverStatus.OPTIMAL ? IEnumSolverStatus.Types.OPTIMAL : IEnumSolverStatus.Types.FEASIBLE,
                solution,
                List.of(),
                null,
                new Metrics(Duration.between(started, Instant.now()).toMillis(),
                        solver.numBranches(), options.numWorkers()));
    }

    private boolean isResourceCandidate(Task task, Resource resource) {
        List<String> selectors = task.candidates().resourceSelectors();
        return selectors.isEmpty() || selectors.stream().anyMatch(selector ->
                "ALL".equalsIgnoreCase(selector) || selector.equals(resource.id()));
    }

    private boolean isSlotCandidate(Task task, Slot slot) {
        List<String> selectors = task.candidates().slotSelectors();
        return selectors.isEmpty() || selectors.stream().anyMatch(selector ->
                "ALL".equalsIgnoreCase(selector) || selector.equals(slot.id()));
    }

    private boolean hasCapabilities(Task task, Resource resource) {
        List<String> required = task.candidates().capabilityTags();
        if (required.isEmpty()) {
            return true;
        }
        Object value = resource.attributes().get("capabilities");
        return value instanceof List<?> capabilities && capabilities.containsAll(required);
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

    private SolveResult infeasibleResult(
            Instant started, SolverOptions options, List<String> infeasibilityCore) {
        return new SolveResult(
                IEnumSolverStatus.Types.INFEASIBLE,
                null,
                List.of(),
                new Diagnostics("No feasible assignment satisfies the active hard constraints",
                        infeasibilityCore, Map.of()),
                new Metrics(Duration.between(started, Instant.now()).toMillis(), 0, options.numWorkers()));
    }

    private SolveResult unknownResult(
            Instant started, SolverOptions options, Set<String> relaxedIds, String message) {
        return new SolveResult(
                IEnumSolverStatus.Types.UNKNOWN,
                null,
                List.of(),
                new Diagnostics(message, List.copyOf(relaxedIds), Map.of()),
                new Metrics(Duration.between(started, Instant.now()).toMillis(), 0, options.numWorkers()));
    }

    private SolveResult withRelaxations(SolveResult result, List<Constraint> relaxedConstraints) {
        if (relaxedConstraints.isEmpty()) {
            return result;
        }
        List<Relaxation> relaxations = relaxedConstraints.stream()
                .map(constraint -> new Relaxation(
                        constraint.id(), constraint.enforcement().name(), "RELAXED", 0.0, true))
                .toList();
        return new SolveResult(result.status(), result.solution(), relaxations,
                result.diagnostics(), result.metrics());
    }

    private record Choice(BoolVar variable, Task task, Resource resource, Slot slot) {
    }
}
