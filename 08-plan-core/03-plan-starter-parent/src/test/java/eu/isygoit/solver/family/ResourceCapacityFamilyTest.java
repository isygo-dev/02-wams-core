package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.Task;
import eu.isygoit.dto.request.TimeWindow;
import eu.isygoit.dto.request.params.ResourceCapacityParams;
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceCapacityFamilyTest {

    private static final Instant T0 = Instant.parse("2026-01-05T00:00:00Z");
    private static final Horizon H = new Horizon(T0, T0.plusSeconds(6000), 60); // 100 ticks of one minute

    private final ResourceCapacityFamily family = new ResourceCapacityFamily();

    private static Instant at(int tick) {
        return T0.plusSeconds(tick * 60L);
    }

    private static Resource resource(String id, int capacity) {
        return new Resource(id, "crew", id, capacity, null, null, null, null, null, null, null, null, null);
    }

    private static Task task(String id) {
        return new Task(id, null, null, 5, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private static PlanningProblem problem(List<Constraint> constraints) {
        return new PlanningProblem(1L, "test", "UTC", H, List.of(), List.of(), List.of(),
                List.of(resource("unary", 1), resource("pool", 3)), List.of(task("a"), task("b")), constraints, List.of(), null, null);
    }

    private static Constraint constraint(IEnumEnforcement.Types e) {
        return new Constraint("cap", "resource_capacity", e, ConstraintScope.all(), Map.of(), null, 1, 0);
    }

    private ValidationContext validation(Constraint c) {
        ValidationContext ctx = mock(ValidationContext.class);
        when(ctx.problem()).thenReturn(problem(List.of(c)));
        return ctx;
    }

    @Test
    void identity() {
        assertEquals("resource_capacity", family.type());
        assertFalse(family.supportedEnforcements().contains(IEnumEnforcement.Types.SOFT));
        assertTrue(family.supportedEnforcements().contains(IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION));
        assertTrue(family.supportedEnforcements().contains(IEnumEnforcement.Types.STRICTLY_REQUIRED));
    }

    @Test
    void validateReportsMissingNegativeCapacityAndBadWindow() {
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED);
        ValidationContext ctx = validation(c);
        family.validate(ctx, c, new ResourceCapacityParams(null, null));
        verify(ctx).error(eq("PARAMS_INVALID"), eq("constraints[0].params.capacity"), anyString());

        ValidationContext ctx2 = validation(c);
        family.validate(ctx2, c, new ResourceCapacityParams(-1, new TimeWindow(at(10), at(5))));
        verify(ctx2).error(eq("PARAMS_INVALID"), eq("constraints[0].params.capacity"), anyString());
        verify(ctx2).error(eq("PARAMS_INVALID"), eq("constraints[0].params.window"), anyString());

        ValidationContext ctx3 = validation(c);
        family.validate(ctx3, c, new ResourceCapacityParams(0, new TimeWindow(T0.plusSeconds(30), at(5))));
        verify(ctx3).error(eq("TIME_NOT_ALIGNED"), eq("constraints[0].params.window.start"), anyString());

        ValidationContext ctx4 = validation(c);
        family.validate(ctx4, c, new ResourceCapacityParams(0, new TimeWindow(at(-5), at(50))));
        verify(ctx4).warning(eq("OUT_OF_HORIZON_CLIPPED"), anyString(), anyString());
        verify(ctx4, never()).error(anyString(), anyString(), anyString());
    }

    @Test
    void newCapacityAndWindowTicks() {
        assertEquals(2, ResourceCapacityFamily.newCapacity(3, new ResourceCapacityParams(2, null)));
        assertEquals(3, ResourceCapacityFamily.newCapacity(3, new ResourceCapacityParams(9, null)));
        assertArrayEquals(new int[]{0, 100}, ResourceCapacityFamily.windowTicks(H, new ResourceCapacityParams(1, null)));
        assertArrayEquals(new int[]{10, 40},
                ResourceCapacityFamily.windowTicks(H, new ResourceCapacityParams(1, new TimeWindow(at(10), at(40)))));
        assertArrayEquals(new int[]{0, 100},
                ResourceCapacityFamily.windowTicks(H, new ResourceCapacityParams(1, new TimeWindow(at(-10), at(400)))));
    }

    @Test
    void estimateCountsOnlyResourcesWhoseCapacityIsLowered() {
        CandidateIndexView idx = mock(CandidateIndexView.class);
        when(idx.resolveResources(any())).thenReturn(new int[]{0, 1});
        when(idx.capacity(0)).thenReturn(1);
        when(idx.capacity(1)).thenReturn(3);
        ModelCost cost = family.estimate(idx, constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED), new ResourceCapacityParams(2, null));
        assertEquals(1, cost.intervals()); // only the pool drops from 3 to 2
        assertEquals(1, cost.constraints());
    }

    @Test
    void preprocessPrunesCandidatesThatNeverFitTheLoweredCapacityOnTheWholeHorizon() {
        ResourceCapacityParams p = new ResourceCapacityParams(1, null);
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED);
        PlanningProblem problem = problem(List.of(c));
        CandidateIndexBuilder.Mutable m = mock(CandidateIndexBuilder.Mutable.class);
        when(m.problem()).thenReturn(problem);
        when(m.candidateBegin(0)).thenReturn(0);
        when(m.candidateEnd(0)).thenReturn(2);
        when(m.candidateBegin(1)).thenReturn(2);
        when(m.candidateEnd(1)).thenReturn(3);
        when(m.candidateResource(0)).thenReturn(1); // pool lowered 3 -> 1
        when(m.candidateResource(1)).thenReturn(1);
        when(m.candidateResource(2)).thenReturn(1);
        when(m.candidateDuration(anyInt())).thenReturn(5);
        when(m.candidateAmount(0)).thenReturn(2); // does not fit
        when(m.candidateAmount(1)).thenReturn(1); // fits
        when(m.candidateAmount(2)).thenReturn(3); // does not fit
        PreprocessContext ctx = mock(PreprocessContext.class);
        when(ctx.candidates()).thenReturn(m);
        when(ctx.resolveResources(any())).thenReturn(new int[]{1});

        family.preprocess(ctx, c, p);

        verify(ctx).pruneCandidate(0, "CAPACITY_AMOUNT", "cap");
        verify(ctx, never()).pruneCandidate(eq(1), anyString(), anyString());
        verify(ctx).pruneCandidate(2, "CAPACITY_AMOUNT", "cap");
    }

    @Test
    void preprocessDoesNothingForAWindowedReduction() {
        PreprocessContext ctx = mock(PreprocessContext.class);
        family.preprocess(ctx, constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED), new ResourceCapacityParams(1, new TimeWindow(at(0), at(10))));
        verify(ctx, never()).pruneCandidate(anyInt(), anyString(), anyString());
    }

    // ------------------------------------------------------------------ checker

    /** tasks[k] = {start, end, resource, amount}. */
    private static SolutionView solution(int[][] tasks) {
        SolutionView sol = mock(SolutionView.class);
        when(sol.taskCount()).thenReturn(tasks.length);
        when(sol.start(anyInt())).thenAnswer(i -> tasks[i.getArgument(0, Integer.class)][0]);
        when(sol.end(anyInt())).thenAnswer(i -> tasks[i.getArgument(0, Integer.class)][1]);
        when(sol.resourceCount(anyInt())).thenReturn(1);
        when(sol.resourceAt(anyInt(), anyInt())).thenAnswer(i -> tasks[i.getArgument(0, Integer.class)][2]);
        when(sol.amountAt(anyInt(), anyInt())).thenAnswer(i -> tasks[i.getArgument(0, Integer.class)][3]);
        when(sol.tasksOfResource(anyInt())).thenAnswer(i -> {
            int r = i.getArgument(0, Integer.class);
            List<Integer> list = new ArrayList<>();
            for (int t = 0; t < tasks.length; t++) {
                if (tasks[t][2] == r) {
                    list.add(t);
                }
            }
            list.sort((a, b) -> Integer.compare(tasks[a][0], tasks[b][0]));
            return list.stream().mapToInt(Integer::intValue).toArray();
        });
        return sol;
    }

    private CheckContext checkContext(SolutionView sol) {
        CandidateIndexView idx = mock(CandidateIndexView.class);
        when(idx.resolveResources(any())).thenReturn(new int[]{0, 1});
        when(idx.capacity(0)).thenReturn(1);
        when(idx.capacity(1)).thenReturn(3);
        when(idx.resourceId(0)).thenReturn("unary");
        when(idx.resourceId(1)).thenReturn("pool");
        CheckContext ctx = mock(CheckContext.class);
        when(ctx.problem()).thenReturn(problem(List.of()));
        when(ctx.index()).thenReturn(idx);
        when(ctx.solution()).thenReturn(sol);
        return ctx;
    }

    @Test
    void checkerMeasuresOverloadedTicksWithinTheWindow() {
        // pool capacity 3 lowered to 1 in [10, 30); a: [5,20) amount 1, b: [15,25) amount 1 -> load 2 on [15,20) = 5 ticks over
        ResourceCapacityParams p = new ResourceCapacityParams(1, new TimeWindow(at(10), at(30)));
        SolutionView sol = solution(new int[][]{{5, 20, 1, 1}, {15, 25, 1, 1}});
        List<String> out = new ArrayList<>();
        family.checker().check(checkContext(sol), constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED), p,
                (t, r, code, msg, m) -> out.add(code + ":" + t + ":" + r + ":" + m));
        assertEquals(List.of("CAPACITY_REDUCTION_EXCEEDED:null:pool:5"), out);
    }

    @Test
    void checkerReportsNothingWhenWithinLoweredCapacityOrOutsideTheWindow() {
        ResourceCapacityParams p = new ResourceCapacityParams(1, new TimeWindow(at(40), at(60)));
        SolutionView sol = solution(new int[][]{{5, 20, 1, 1}, {15, 25, 1, 1}}); // overlap happens before the window
        List<String> out = new ArrayList<>();
        family.checker().check(checkContext(sol), constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED), p,
                (t, r, code, msg, m) -> out.add(code));
        assertTrue(out.isEmpty());
    }

    @Test
    void checkerTreatsAUnaryResourceReducedToZeroAsFullyBlockedAndIgnoresZeroDurationTasks() {
        ResourceCapacityParams p = new ResourceCapacityParams(0, null);
        SolutionView sol = solution(new int[][]{{10, 14, 0, 1}, {50, 50, 0, 1}}); // second task has zero duration
        List<String> out = new ArrayList<>();
        family.checker().check(checkContext(sol), constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED), p,
                (t, r, code, msg, m) -> out.add(code + ":" + r + ":" + m));
        assertEquals(List.of("CAPACITY_REDUCTION_EXCEEDED:unary:4"), out);
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
