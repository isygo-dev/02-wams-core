package eu.isygoit.plan.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.enums.IEnumSolverStatus;
import eu.isygoit.plan.api.DomainDefinition;
import eu.isygoit.plan.api.PlanningMeta;
import eu.isygoit.plan.api.PlanningRequest;
import eu.isygoit.plan.api.TypeDefinition;
import eu.isygoit.plan.domain.Assignment;
import eu.isygoit.plan.domain.Constraint;
import eu.isygoit.plan.domain.ConstraintScope;
import eu.isygoit.plan.domain.Diagnostics;
import eu.isygoit.plan.domain.Metrics;
import eu.isygoit.plan.domain.PlanningProblem;
import eu.isygoit.plan.domain.PlanningSolution;
import eu.isygoit.plan.domain.Resource;
import eu.isygoit.plan.domain.ResourceAssignment;
import eu.isygoit.plan.domain.Slot;
import eu.isygoit.plan.domain.SolveResult;
import eu.isygoit.plan.domain.SolverOptions;
import eu.isygoit.plan.domain.Task;
import eu.isygoit.plan.domain.UnassignedTask;
import eu.isygoit.plan.engine.ConstraintRegistry;
import eu.isygoit.plan.engine.ObjectiveRegistry;
import eu.isygoit.plan.engine.SolverEngine;
import eu.isygoit.plan.engine.SolverRegistry;
import eu.isygoit.plan.engine.handler.ResourceCapacityConstraintHandler;
import eu.isygoit.plan.engine.handler.TaskEligibilityConstraintHandler;
import eu.isygoit.plan.exception.InfeasibleException;
import eu.isygoit.plan.exception.InvalidRequestException;
import eu.isygoit.plan.exception.InvalidSolverResultException;
import eu.isygoit.plan.exception.UnknownConstraintException;
import eu.isygoit.plan.exception.UnknownSolverException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanningServiceTest {
    private static final String TEST_ENGINE = "test-engine";
    private static final Instant SLOT_START = Instant.parse("2026-01-01T09:00:00Z");
    private static final Instant SLOT_END = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void appliesDefaultsToOmittedSolverOptionsFields() throws Exception {
        SolverOptions options = new ObjectMapper().readValue(
                "{\"engine\":\"timefold\",\"maxTimeSeconds\":45}",
                SolverOptions.class);

        assertEquals(new SolverOptions("timefold", 45,
                SolverOptions.DEFAULT_NUM_WORKERS, SolverOptions.DEFAULT_SEED,
                SolverOptions.DEFAULT_LOG_SEARCH, List.of()), options);
    }

    @Test
    void rejectsExplicitInvalidSolverOptionsValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new SolverOptions(null, 0, null, null, null, null));
    }

    @Test
    void preservesJsonNullValuesInGenericMaps() throws Exception {
        Map<String, Object> attributes = new ObjectMapper().readValue(
                "{\"nullable\":null}",
                new TypeReference<Map<String, Object>>() {
                });
        Task task = new Task("task", "type", attributes, null);
        Resource resource = new Resource("resource", "type", attributes, List.of());
        Slot slot = new Slot("slot", "type", null, null, attributes);
        Constraint constraint = new Constraint(
                "constraint", "test", IEnumEnforcement.Types.SOFT, null, attributes, null, 1);
        Diagnostics diagnostics = new Diagnostics("test", List.of(), attributes);

        assertNull(task.attributes().get("nullable"));
        assertTrue(task.attributes().containsKey("nullable"));
        assertNull(resource.attributes().get("nullable"));
        assertTrue(resource.attributes().containsKey("nullable"));
        assertNull(slot.attributes().get("nullable"));
        assertTrue(slot.attributes().containsKey("nullable"));
        assertNull(constraint.params().get("nullable"));
        assertTrue(constraint.params().containsKey("nullable"));
        assertNull(diagnostics.details().get("nullable"));
        assertTrue(diagnostics.details().containsKey("nullable"));
    }

    @Test
    void solvesValidatedRequestAndAcceptsAValidOptionalTaskResult() {
        PlanningRequest request = request(TEST_ENGINE, true);
        SolveResult expected = optimalResult(
                List.of(assignment("required-task")),
                List.of(new UnassignedTask(
                        "optional-task", "OPTIONAL_NOT_SELECTED", "Not selected", null)));
        TestHarness harness = harness(expected);

        SolveResult actual = harness.service().solve(request);

        assertSame(expected, actual);
        assertEquals(1, harness.engine().calls());
        assertEquals("generic-planning", harness.engine().lastProblem().domainName());
        assertEquals(List.of("required-task", "optional-task"),
                harness.engine().lastProblem().tasks().stream().map(Task::id).toList());
    }

    @Test
    void rejectsMalformedRequestBeforeInvokingSolver() {
        PlanningRequest request = requestWithUnknownTaskType(TEST_ENGINE);
        TestHarness harness = harness(optimalResult(List.of(), List.of()));

        assertThrows(InvalidRequestException.class, () -> harness.service().solve(request));

        assertEquals(0, harness.engine().calls());
    }

    @Test
    void rejectsUnsupportedConstraintBeforeInvokingSolver() {
        PlanningRequest request = requestWithUnsupportedConstraint(TEST_ENGINE);
        TestHarness harness = harness(optimalResult(List.of(), List.of()));

        assertThrows(UnknownConstraintException.class, () -> harness.service().solve(request));

        assertEquals(0, harness.engine().calls());
    }

    @Test
    void rejectsAnUnregisteredSolver() {
        PlanningRequest request = request("unregistered-engine", false);
        TestHarness harness = harness(optimalResult(List.of(assignment("required-task")), List.of()));

        assertThrows(UnknownSolverException.class, () -> harness.service().solve(request));

        assertEquals(0, harness.engine().calls());
    }

    @Test
    void rejectsAnUnregisteredSolverDuringSubmissionPreflight() {
        TestHarness harness = harness(optimalResult(List.of(), List.of()));

        assertThrows(
                UnknownSolverException.class,
                () -> harness.service().normalizeAndValidate(request("unregistered-engine", false)));

        assertEquals(0, harness.engine().calls());
    }

    @Test
    void convertsInfeasibleSolverResultToInfeasibleException() {
        Diagnostics diagnostics = new Diagnostics(
                "No feasible assignment", List.of("required-task"), Map.of());
        SolveResult infeasible = new SolveResult(
                IEnumSolverStatus.Types.INFEASIBLE,
                null,
                List.of(),
                diagnostics,
                new Metrics(1, 0, 1));
        TestHarness harness = harness(infeasible);

        InfeasibleException exception = assertThrows(
                InfeasibleException.class,
                () -> harness.service().solve(request(TEST_ENGINE, false)));

        assertSame(diagnostics, exception.diagnostics());
        assertEquals(1, harness.engine().calls());
    }

    @Test
    void rejectsAResultThatLeavesARequiredTaskUnassigned() {
        TestHarness harness = harness(optimalResult(List.of(), List.of()));

        assertThrows(
                InvalidSolverResultException.class,
                () -> harness.service().solve(request(TEST_ENGINE, false)));
    }

    @Test
    void rejectsAResultThatViolatesTaskEligibility() {
        PlanningRequest request = requestWithSlotEligibility();
        TestHarness harness = harness(optimalResult(List.of(assignment("required-task")), List.of()));

        assertThrows(InvalidSolverResultException.class, () -> harness.service().solve(request));
    }

    @Test
    void rejectsAResultThatExceedsResourceCapacity() {
        PlanningRequest request = requestWithCapacityOne();
        SolveResult invalidResult = optimalResult(
                List.of(assignment("required-task"), assignment("second-required-task")),
                List.of());
        TestHarness harness = harness(invalidResult);

        assertThrows(InvalidSolverResultException.class, () -> harness.service().solve(request));
    }

    @Test
    void returnsUnknownResultWithoutPassingItToSolutionValidation() {
        SolveResult unknown = new SolveResult(
                IEnumSolverStatus.Types.UNKNOWN,
                null,
                List.of(),
                new Diagnostics("Time limit reached", List.of(), Map.of()),
                new Metrics(1, 0, 1));
        TestHarness harness = harness(unknown);

        SolveResult actual = harness.service().solve(request(TEST_ENGINE, false));

        assertSame(unknown, actual);
        assertEquals(1, harness.engine().calls());
    }

    @Test
    void propagatesUnexpectedSolverFailure() {
        TestHarness harness = harness(problem -> {
            throw new IllegalStateException("solver failed");
        });

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> harness.service().solve(request(TEST_ENGINE, false)));

        assertEquals("solver failed", exception.getMessage());
        assertEquals(1, harness.engine().calls());
    }

    private TestHarness harness(SolveResult result) {
        return harness(problem -> result);
    }

    private TestHarness harness(Function<PlanningProblem, SolveResult> solve) {
        StubSolverEngine engine = new StubSolverEngine(TEST_ENGINE, solve);
        ConstraintRegistry constraintRegistry = new ConstraintRegistry(List.of(
                new ResourceCapacityConstraintHandler(),
                new TaskEligibilityConstraintHandler()));
        PlanningService service = new PlanningService(
                new PlanningRequestValidator(),
                new DomainNormalizer(),
                constraintRegistry,
                new ObjectiveRegistry(List.of()),
                new SolverRegistry(List.of(engine)),
                new PlanningSolutionValidator(constraintRegistry));
        return new TestHarness(service, engine);
    }

    private PlanningRequest request(String engine, boolean includeOptionalTask) {
        List<Task> tasks = includeOptionalTask
                ? List.of(task("required-task", true), task("optional-task", false))
                : List.of(task("required-task", true));
        return new PlanningRequest(
                "tenant-a",
                new PlanningMeta(UUID.randomUUID(), "1.0", "generic-planning", "en", "UTC"),
                new DomainDefinition(
                        List.of(new TypeDefinition("resource", List.of())),
                        List.of(new TypeDefinition("task", List.of())),
                        List.of(new TypeDefinition("slot", List.of()))),
                List.of(new Slot("slot-1", "slot", SLOT_START, SLOT_END, Map.of())),
                List.of(new Resource("resource-1", "resource", Map.of("capacity", 2), List.of())),
                tasks,
                List.of(),
                List.of(),
                new SolverOptions(engine, 30, 1, 0L, false, List.of()));
    }

    private PlanningRequest requestWithUnknownTaskType(String engine) {
        PlanningRequest valid = request(engine, false);
        return new PlanningRequest(
                valid.tenant(),
                valid.meta(),
                valid.domain(),
                valid.slots(),
                valid.resources(),
                List.of(task("required-task", "unknown-task-type", true)),
                valid.constraints(),
                valid.objectives(),
                valid.solverOptions());
    }

    private PlanningRequest requestWithUnsupportedConstraint(String engine) {
        PlanningRequest valid = request(engine, false);
        Constraint unsupported = new Constraint(
                "constraint-1",
                "unsupported_constraint",
                IEnumEnforcement.Types.STRICTLY_REQUIRED,
                null,
                Map.of(),
                null,
                1);
        return new PlanningRequest(
                valid.tenant(),
                valid.meta(),
                valid.domain(),
                valid.slots(),
                valid.resources(),
                valid.tasks(),
                List.of(unsupported),
                valid.objectives(),
                valid.solverOptions());
    }

    private PlanningRequest requestWithSlotEligibility() {
        PlanningRequest valid = request(TEST_ENGINE, false);
        Constraint eligibility = new Constraint(
                "task-slot-eligibility",
                "task_eligibility",
                IEnumEnforcement.Types.STRICTLY_REQUIRED,
                new ConstraintScope("task", "required-task"),
                Map.of("slotIds", List.of("slot-2")),
                null,
                1);
        return new PlanningRequest(
                valid.tenant(),
                valid.meta(),
                valid.domain(),
                List.of(
                        new Slot("slot-1", "slot", SLOT_START, SLOT_END, Map.of()),
                        new Slot("slot-2", "slot", SLOT_START.plusSeconds(3600), SLOT_END.plusSeconds(3600), Map.of())),
                valid.resources(),
                valid.tasks(),
                List.of(eligibility),
                valid.objectives(),
                valid.solverOptions());
    }

    private PlanningRequest requestWithCapacityOne() {
        PlanningRequest valid = request(TEST_ENGINE, false);
        return new PlanningRequest(
                valid.tenant(),
                valid.meta(),
                valid.domain(),
                valid.slots(),
                List.of(new Resource("resource-1", "resource", Map.of("capacity", 1), List.of())),
                List.of(task("required-task", true), task("second-required-task", true)),
                valid.constraints(),
                valid.objectives(),
                valid.solverOptions());
    }

    private Task task(String id, boolean required) {
        return task(id, "task", required);
    }

    private Task task(String id, String typeId, boolean required) {
        return new Task(id, typeId, Map.of(), null, required);
    }

    private Assignment assignment(String taskId) {
        return new Assignment(
                taskId,
                "slot-1",
                List.of(new ResourceAssignment("resource-1", "default")),
                SLOT_START,
                SLOT_END);
    }

    private SolveResult optimalResult(
            List<Assignment> assignments,
            List<UnassignedTask> unassignedTasks) {
        return new SolveResult(
                IEnumSolverStatus.Types.OPTIMAL,
                new PlanningSolution(assignments, unassignedTasks, List.of(), List.of(), List.of(), 1.0),
                List.of(),
                null,
                new Metrics(1, 0, 1));
    }

    private static final class StubSolverEngine implements SolverEngine {
        private final String name;
        private final Function<PlanningProblem, SolveResult> solve;
        private final AtomicInteger callCount = new AtomicInteger();
        private PlanningProblem lastProblem;

        private StubSolverEngine(String name, Function<PlanningProblem, SolveResult> solve) {
            this.name = name;
            this.solve = solve;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public SolveResult solve(PlanningProblem problem, SolverOptions options) {
            lastProblem = problem;
            callCount.incrementAndGet();
            return solve.apply(problem);
        }

        private int calls() {
            return callCount.get();
        }

        private PlanningProblem lastProblem() {
            return lastProblem;
        }
    }

    private record TestHarness(PlanningService service, StubSolverEngine engine) {
    }
}
