package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.Slot;
import eu.isygoit.dto.request.params.TaskEligibilityParams;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.index.CandidateIndexBuilder;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.CheckContext;
import eu.isygoit.solver.spi.ModelCost;
import eu.isygoit.solver.spi.PreprocessContext;
import eu.isygoit.solver.spi.SolutionView;
import eu.isygoit.solver.spi.ValidationContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskEligibilityFamilyTest {

    private static final Instant T0 = Instant.parse("2026-01-05T00:00:00Z");
    private static final Horizon H = new Horizon(T0, T0.plusSeconds(6000), 60); // 100 ticks of one minute

    private final TaskEligibilityFamily family = new TaskEligibilityFamily();

    private static Instant at(int tick) {
        return T0.plusSeconds(tick * 60L);
    }

    private static Resource resource(String id, String type, Set<String> skills) {
        return new Resource(id, type, id, 1, skills, null, null, null, null, null, null, null, null);
    }

    private static Slot slot(String id, int from, int to) {
        return new Slot(id, "period", id, at(from), at(to), 0.0, null);
    }

    private static PlanningProblem problem(List<Constraint> constraints) {
        return new PlanningProblem(1L, "test", "UTC", H,
                List.of(slot("s1", 0, 10), slot("s2", 20, 30)), List.of(), List.of(),
                List.of(resource("alice", "teacher", Set.of("math")), resource("bob", "teacher", Set.of("art")),
                        resource("room", "room", Set.of())),
                List.of(), constraints, List.of(), null, null);
    }

    private static Constraint constraint(IEnumEnforcement.Types enforcement, TaskEligibilityParams p) {
        return new Constraint("c1", "task_eligibility", enforcement, ConstraintScope.all(), Map.of(), null, 3, 0);
    }

    private static TaskEligibilityParams params(List<String> ids, List<String> exclude, List<String> types, List<String> skills,
                                                List<String> slots) {
        return new TaskEligibilityParams(ids, exclude, types, skills, slots);
    }

    private ValidationContext validationContext(Constraint c, PlanningProblem problem) {
        ValidationContext ctx = mock(ValidationContext.class);
        when(ctx.problem()).thenReturn(problem);
        when(ctx.hasResource(anyString())).thenAnswer(i -> problem.resources().stream().anyMatch(r -> r.id().equals(i.getArgument(0))));
        when(ctx.hasSlot(anyString())).thenAnswer(i -> problem.slots().stream().anyMatch(s -> s.id().equals(i.getArgument(0))));
        return ctx;
    }

    @Test
    void identityAndEnforcements() {
        assertEquals("task_eligibility", family.type());
        assertEquals(TaskEligibilityParams.class, family.paramsType());
        assertEquals(4, family.supportedEnforcements().size());
        assertEquals("plan.constraint.task_eligibility.description", family.descriptionKey());
    }

    @Test
    void validateRejectsEmptyParamsAndUnknownReferences() {
        TaskEligibilityParams empty = params(null, null, null, null, null);
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED, empty);
        PlanningProblem problem = problem(List.of(c));
        ValidationContext ctx = validationContext(c, problem);
        family.validate(ctx, c, empty);
        verify(ctx).error(eq("PARAMS_INVALID"), anyString(), anyString());

        TaskEligibilityParams bad = params(List.of("ghost"), List.of("ghost2"), List.of("nothing"), null, List.of("s9"));
        ValidationContext ctx2 = validationContext(c, problem);
        family.validate(ctx2, c, bad);
        verify(ctx2).error(eq("UNKNOWN_REFERENCE"), eq("constraints[0].params.resourceIds[0]"), anyString());
        verify(ctx2).error(eq("UNKNOWN_REFERENCE"), eq("constraints[0].params.excludeResourceIds[0]"), anyString());
        verify(ctx2).error(eq("UNKNOWN_REFERENCE"), eq("constraints[0].params.slotIds[0]"), anyString());
        verify(ctx2).warning(eq("UNKNOWN_REFERENCE"), eq("constraints[0].params.resourceTypeIds[0]"), anyString());
    }

    @Test
    void validateAcceptsAllLiteralAndKnownIds() {
        TaskEligibilityParams ok = params(List.of("ALL"), List.of("bob"), null, null, List.of("s1"));
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED, ok);
        ValidationContext ctx = validationContext(c, problem(List.of(c)));
        family.validate(ctx, c, ok);
        verify(ctx, never()).error(anyString(), anyString(), anyString());
    }

    @Test
    void allowedResourcesAppliesAllCriteria() {
        PlanningProblem problem = problem(List.of());
        assertArrayEquals(new boolean[]{true, false, false},
                TaskEligibilityFamily.allowedResources(problem, params(null, null, null, List.of("math"), null)));
        assertArrayEquals(new boolean[]{true, true, false},
                TaskEligibilityFamily.allowedResources(problem, params(null, null, List.of("teacher"), null, null)));
        assertArrayEquals(new boolean[]{true, false, true},
                TaskEligibilityFamily.allowedResources(problem, params(List.of("alice", "room"), null, null, null, null)));
        assertArrayEquals(new boolean[]{false, true, true},
                TaskEligibilityFamily.allowedResources(problem, params(List.of("ALL"), List.of("alice"), null, null, null)));
    }

    @Test
    void allowedSlotStartsAreTicksOfTheListedSlots() {
        PlanningProblem problem = problem(List.of());
        assertArrayEquals(new int[]{20}, TaskEligibilityFamily.allowedSlotStarts(problem, params(null, null, null, null, List.of("s2"))));
        assertNull(TaskEligibilityFamily.allowedSlotStarts(problem, params(null, null, null, null, List.of("ALL"))));
        assertNull(TaskEligibilityFamily.allowedSlotStarts(problem, params(null, null, null, null, null)));
    }

    @Test
    void estimateIsZeroForHardAndCountsCandidatesOtherwise() {
        TaskEligibilityParams p = params(List.of("alice"), null, null, null, null);
        CandidateIndexView idx = mock(CandidateIndexView.class);
        when(idx.resolveTasks(any())).thenReturn(new int[]{0, 1});
        when(idx.candidateBegin(0)).thenReturn(0);
        when(idx.candidateEnd(0)).thenReturn(3);
        when(idx.candidateBegin(1)).thenReturn(3);
        when(idx.candidateEnd(1)).thenReturn(5);
        assertEquals(ModelCost.ZERO, family.estimate(idx, constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED, p), p));
        ModelCost soft = family.estimate(idx, constraint(IEnumEnforcement.Types.SOFT, p), p);
        assertEquals(7, soft.constraints()); // 5 candidates + 2 slot restrictions (upper bound)
        assertTrue(soft.boolVars() >= 5);
        ModelCost relaxable = family.estimate(idx, constraint(IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION, p), p);
        assertEquals(7, relaxable.constraints());
    }

    @Test
    void preprocessPrunesForbiddenCandidatesAndRestrictsSlotStarts() {
        TaskEligibilityParams p = params(List.of("alice"), null, null, null, List.of("s2"));
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED, p);
        PlanningProblem problem = problem(List.of(c));
        CandidateIndexBuilder.Mutable mutable = mock(CandidateIndexBuilder.Mutable.class);
        when(mutable.problem()).thenReturn(problem);
        when(mutable.candidateBegin(0)).thenReturn(0);
        when(mutable.candidateEnd(0)).thenReturn(3);
        when(mutable.candidateResource(0)).thenReturn(0); // alice: allowed
        when(mutable.candidateResource(1)).thenReturn(1); // bob: pruned
        when(mutable.candidateResource(2)).thenReturn(2); // room: pruned
        PreprocessContext ctx = mock(PreprocessContext.class);
        when(ctx.candidates()).thenReturn(mutable);
        when(ctx.resolveTasks(any())).thenReturn(new int[]{0});

        family.preprocess(ctx, c, p);

        verify(ctx, never()).pruneCandidate(eq(0), anyString(), anyString());
        verify(ctx).pruneCandidate(1, "ELIGIBILITY", "c1");
        verify(ctx).pruneCandidate(2, "ELIGIBILITY", "c1");
        verify(ctx).restrictStartDomain(eq(0), eq(new int[]{20, 20}));
    }

    // ------------------------------------------------------------------ checker

    private static SolutionView solution(int[] start, int[][] resources) {
        SolutionView sol = mock(SolutionView.class);
        when(sol.taskCount()).thenReturn(start.length);
        when(sol.selected(anyInt())).thenAnswer(i -> start[i.getArgument(0, Integer.class)] >= 0);
        when(sol.start(anyInt())).thenAnswer(i -> start[i.getArgument(0, Integer.class)]);
        when(sol.resourceCount(anyInt())).thenAnswer(i -> resources[i.getArgument(0, Integer.class)].length);
        when(sol.resourceAt(anyInt(), anyInt())).thenAnswer(i -> resources[i.getArgument(0, Integer.class)][i.getArgument(1, Integer.class)]);
        return sol;
    }

    private CheckContext checkContext(PlanningProblem problem, SolutionView sol) {
        CandidateIndexView idx = mock(CandidateIndexView.class);
        when(idx.resolveTasks(any())).thenReturn(new int[]{0, 1, 2});
        when(idx.taskId(anyInt())).thenAnswer(i -> "t" + i.getArgument(0));
        when(idx.resourceId(anyInt())).thenAnswer(i -> problem.resources().get(i.getArgument(0, Integer.class)).id());
        CheckContext ctx = mock(CheckContext.class);
        when(ctx.problem()).thenReturn(problem);
        when(ctx.index()).thenReturn(idx);
        when(ctx.solution()).thenReturn(sol);
        return ctx;
    }

    @Test
    void checkerCountsOneUnitPerForbiddenAssignmentAndPerForbiddenSlotStart() {
        TaskEligibilityParams p = params(List.of("alice"), null, null, null, List.of("s2")); // only alice, only start 20
        Constraint c = constraint(IEnumEnforcement.Types.SOFT, p);
        PlanningProblem problem = problem(List.of(c));
        // t0: alice @20 (ok); t1: bob @20 (resource violation); t2: alice+room @5 (room violation + slot violation); t3 unselected
        SolutionView sol = solution(new int[]{20, 20, 5}, new int[][]{{0}, {1}, {0, 2}});
        List<String> out = new ArrayList<>();
        family.checker().check(checkContext(problem, sol), c, p, (t, r, code, msg, m) -> out.add(code + ":" + t + ":" + r + ":" + m));
        assertEquals(List.of("RESOURCE_NOT_ELIGIBLE:t1:bob:1", "RESOURCE_NOT_ELIGIBLE:t2:room:1", "SLOT_NOT_ALLOWED:t2:null:1"), out);
    }

    @Test
    void checkerIgnoresUnselectedTasks() {
        TaskEligibilityParams p = params(List.of("alice"), null, null, null, null);
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED, p);
        SolutionView sol = solution(new int[]{-1, -1, -1}, new int[][]{{1}, {1}, {1}});
        List<String> out = new ArrayList<>();
        family.checker().check(checkContext(problem(List.of(c)), sol), c, p, (t, r, code, msg, m) -> out.add(code));
        assertTrue(out.isEmpty());
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
