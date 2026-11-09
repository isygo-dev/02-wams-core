package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.Task;
import eu.isygoit.dto.request.TimeWindow;
import eu.isygoit.dto.request.params.ResourceAvailabilityParams;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceAvailabilityFamilyTest {

    private static final Instant T0 = Instant.parse("2026-01-05T00:00:00Z");
    private static final Horizon H = new Horizon(T0, T0.plusSeconds(6000), 60); // 100 ticks

    private final ResourceAvailabilityFamily family = new ResourceAvailabilityFamily();

    private static Instant at(int tick) {
        return T0.plusSeconds(tick * 60L);
    }

    private static TimeWindow w(int from, int to) {
        return new TimeWindow(at(from), at(to));
    }

    private static Constraint constraint(IEnumEnforcement.Types e) {
        return new Constraint("av", "resource_availability", e, ConstraintScope.all(), Map.of(), null, 2, 0);
    }

    private static Resource resource(String id) {
        return new Resource(id, "crew", id, 1, null, null, null, null, null, null, null, null, null);
    }

    private static Task task(String id) {
        return new Task(id, null, null, 5, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private static PlanningProblem problem(Constraint c) {
        return new PlanningProblem(1L, "t", "UTC", H, List.of(), List.of(), List.of(), List.of(resource("r0"), resource("r1")),
                List.of(task("a")), List.of(c), List.of(), null, null);
    }

    private static ValidationContext validation(Constraint c) {
        ValidationContext ctx = mock(ValidationContext.class);
        when(ctx.problem()).thenReturn(problem(c));
        return ctx;
    }

    @Test
    void identity() {
        assertEquals("resource_availability", family.type());
        assertEquals(ResourceAvailabilityParams.class, family.paramsType());
        assertEquals(4, family.supportedEnforcements().size());
    }

    @Test
    void validateRejectsEmptyAndBrokenWindows() {
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED);
        ValidationContext empty = validation(c);
        family.validate(empty, c, new ResourceAvailabilityParams(null, null));
        verify(empty).error(eq("PARAMS_INVALID"), eq("constraints[0].params"), anyString());

        ValidationContext inverted = validation(c);
        family.validate(inverted, c, new ResourceAvailabilityParams(List.of(w(30, 10)), null));
        verify(inverted).error(eq("PARAMS_INVALID"), eq("constraints[0].params.unavailable[0]"), anyString());

        ValidationContext unaligned = validation(c);
        family.validate(unaligned, c, new ResourceAvailabilityParams(null, List.of(new TimeWindow(T0.plusSeconds(10), at(30)))));
        verify(unaligned).error(eq("TIME_NOT_ALIGNED"), eq("constraints[0].params.availableOnly[0].start"), anyString());

        ValidationContext clipped = validation(c);
        family.validate(clipped, c, new ResourceAvailabilityParams(List.of(w(-10, 10)), null));
        verify(clipped).warning(eq("OUT_OF_HORIZON_CLIPPED"), anyString(), anyString());
        verify(clipped, never()).error(anyString(), anyString(), anyString());
    }

    @Test
    void blockedRunsMergeUnavailableWindowsAndClipToTheHorizon() {
        ResourceAvailabilityParams p = new ResourceAvailabilityParams(List.of(w(10, 20), w(15, 30), w(30, 35), w(90, 150), w(-5, 2)), null);
        assertArrayEquals(new int[]{0, 2, 10, 35, 90, 100}, ResourceAvailabilityFamily.blockedRuns(H, p));
    }

    @Test
    void blockedRunsOfAvailableOnlyAreTheComplementInsideTheHorizon() {
        ResourceAvailabilityParams p = new ResourceAvailabilityParams(null, List.of(w(10, 30), w(25, 50), w(70, 80)));
        assertArrayEquals(new int[]{0, 10, 50, 70, 80, 100}, ResourceAvailabilityFamily.blockedRuns(H, p));
        ResourceAvailabilityParams both = new ResourceAvailabilityParams(List.of(w(20, 40)), List.of(w(10, 60)));
        assertArrayEquals(new int[]{0, 10, 20, 40, 60, 100}, ResourceAvailabilityFamily.blockedRuns(H, both));
        ResourceAvailabilityParams outside = new ResourceAvailabilityParams(null, List.of(w(200, 300)));
        assertArrayEquals(new int[]{0, 100}, ResourceAvailabilityFamily.blockedRuns(H, outside));
    }

    @Test
    void forbiddenAndAllowedStartRunsInflateTheBlockedWindowsByTheDuration() {
        int[] blocked = {50, 60};
        assertArrayEquals(new int[]{46, 59}, ResourceAvailabilityFamily.forbiddenStartRuns(blocked, 5));
        assertArrayEquals(new int[]{0, 45, 60, 95}, ResourceAvailabilityFamily.allowedStartRuns(blocked, 5, 100));
        // a window at the very beginning
        assertArrayEquals(new int[]{10, 95}, ResourceAvailabilityFamily.allowedStartRuns(new int[]{0, 10}, 5, 100));
        // a task as long as the horizon cannot avoid anything inside it
        assertArrayEquals(new int[0], ResourceAvailabilityFamily.allowedStartRuns(new int[]{40, 50}, 100, 100));
        assertArrayEquals(new int[0], ResourceAvailabilityFamily.allowedStartRuns(blocked, 101, 100));
        // adjacent forbidden runs merge
        assertArrayEquals(new int[]{0, 4, 32, 94}, ResourceAvailabilityFamily.allowedStartRuns(new int[]{10, 20, 22, 32}, 6, 100));
    }

    @Test
    void estimateIsZeroForHardAndCountsScopedPositiveDurationCandidatesOtherwise() {
        CandidateIndexView idx = mock(CandidateIndexView.class);
        when(idx.resourceCount()).thenReturn(2);
        when(idx.taskCount()).thenReturn(1);
        when(idx.horizon()).thenReturn(H);
        when(idx.resolveResources(any())).thenReturn(new int[]{1});
        when(idx.candidateBegin(0)).thenReturn(0);
        when(idx.candidateEnd(0)).thenReturn(3);
        when(idx.candidateResource(0)).thenReturn(0); // out of scope
        when(idx.candidateResource(1)).thenReturn(1);
        when(idx.candidateResource(2)).thenReturn(1);
        when(idx.candidateDuration(0)).thenReturn(5);
        when(idx.candidateDuration(1)).thenReturn(5);
        when(idx.candidateDuration(2)).thenReturn(0); // zero duration occupies nothing
        ResourceAvailabilityParams p = new ResourceAvailabilityParams(List.of(w(10, 20)), null);
        assertEquals(ModelCost.ZERO, family.estimate(idx, constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED), p));
        ModelCost soft = family.estimate(idx, constraint(IEnumEnforcement.Types.SOFT), p);
        assertEquals(1 + 2, soft.boolVars()); // 1 candidate: 1 instance literal + before/after
        assertEquals(3, soft.constraints());
    }

    @Test
    void preprocessRestrictsCandidateStartsAndPrunesCandidatesThatCannotAvoidBlockedTime() {
        ResourceAvailabilityParams p = new ResourceAvailabilityParams(List.of(w(50, 60)), null);
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED);
        PlanningProblem problem = problem(c);
        CandidateIndexBuilder.Mutable m = mock(CandidateIndexBuilder.Mutable.class);
        when(m.problem()).thenReturn(problem);
        when(m.candidateBegin(0)).thenReturn(0);
        when(m.candidateEnd(0)).thenReturn(4);
        when(m.candidateResource(0)).thenReturn(1);
        when(m.candidateResource(1)).thenReturn(0); // out of scope
        when(m.candidateResource(2)).thenReturn(1);
        when(m.candidateResource(3)).thenReturn(1);
        when(m.candidateDuration(0)).thenReturn(5);
        when(m.candidateDuration(1)).thenReturn(5);
        when(m.candidateDuration(2)).thenReturn(0); // skipped
        when(m.candidateDuration(3)).thenReturn(100); // [0,100) overlaps [50,60) for the only start 0: pruned
        PreprocessContext ctx = mock(PreprocessContext.class);
        when(ctx.candidates()).thenReturn(m);
        when(ctx.horizon()).thenReturn(H);
        when(ctx.resolveResources(any())).thenReturn(new int[]{1});

        family.preprocess(ctx, c, p);

        verify(m).restrictCandidateStart(eq(0), eq(new int[]{0, 45, 60, 95}));
        verify(m, never()).restrictCandidateStart(eq(1), any());
        verify(m, never()).restrictCandidateStart(eq(2), any());
        verify(ctx).pruneCandidate(3, "UNAVAILABLE", "av");
    }

    // ------------------------------------------------------------------ checker

    /** tasks[t] = {start, end, resource}; start < 0 = unselected. */
    private List<String> check(ResourceAvailabilityParams p, int[] scopedResources, int[][] tasks) {
        SolutionView sol = mock(SolutionView.class);
        when(sol.taskCount()).thenReturn(tasks.length);
        when(sol.selected(anyInt())).thenAnswer(i -> tasks[i.getArgument(0, Integer.class)][0] >= 0);
        when(sol.start(anyInt())).thenAnswer(i -> tasks[i.getArgument(0, Integer.class)][0]);
        when(sol.end(anyInt())).thenAnswer(i -> tasks[i.getArgument(0, Integer.class)][1]);
        when(sol.resourceCount(anyInt())).thenReturn(1);
        when(sol.resourceAt(anyInt(), anyInt())).thenAnswer(i -> tasks[i.getArgument(0, Integer.class)][2]);
        CandidateIndexView idx = mock(CandidateIndexView.class);
        when(idx.resourceCount()).thenReturn(2);
        when(idx.resolveResources(any())).thenReturn(scopedResources);
        when(idx.taskId(anyInt())).thenAnswer(i -> "t" + i.getArgument(0));
        when(idx.resourceId(anyInt())).thenAnswer(i -> "r" + i.getArgument(0));
        CheckContext ctx = mock(CheckContext.class);
        when(ctx.problem()).thenReturn(problem(constraint(IEnumEnforcement.Types.SOFT)));
        when(ctx.index()).thenReturn(idx);
        when(ctx.solution()).thenReturn(sol);
        List<String> out = new ArrayList<>();
        family.checker().check(ctx, constraint(IEnumEnforcement.Types.SOFT), p, (t, r, code, msg, m) -> out.add(t + ":" + r + ":" + m));
        return out;
    }

    @Test
    void checkerCountsOneUnitPerAssignmentOverlappingBlockedTimeHoweverManyWindowsItTouches() {
        ResourceAvailabilityParams p = new ResourceAvailabilityParams(List.of(w(10, 20), w(30, 40)), null);
        List<String> out = check(p, new int[]{0}, new int[][]{
                {5, 15, 0},   // overlaps the first window: 1
                {15, 35, 0},  // overlaps both windows: still 1
                {20, 30, 0},  // exactly between the windows: ok (half-open)
                {12, 18, 1},  // overlaps but resource 1 is not in scope
                {-1, -1, 0},  // unselected
                {15, 15, 0}}); // zero duration inside a window occupies nothing
        assertEquals(List.of("t0:r0:1", "t1:r0:1"), out);
    }

    @Test
    void checkerUsesTheComplementForAvailableOnlyWindows() {
        ResourceAvailabilityParams p = new ResourceAvailabilityParams(null, List.of(w(10, 50)));
        List<String> out = check(p, new int[]{0, 1}, new int[][]{{10, 50, 0}, {5, 12, 1}, {45, 55, 0}});
        assertEquals(List.of("t1:r1:1", "t2:r0:1"), out);
        assertTrue(check(new ResourceAvailabilityParams(List.of(w(0, 0)), null), new int[]{0}, new int[][]{{0, 5, 0}}).isEmpty());
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
