package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.params.NoOverlapParams;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.CheckContext;
import eu.isygoit.solver.spi.ModelCost;
import eu.isygoit.solver.spi.SolutionView;
import eu.isygoit.solver.spi.ValidationContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

class NoOverlapFamilyTest {

    private static final Instant T0 = Instant.parse("2026-01-05T00:00:00Z");
    private static final Horizon H = new Horizon(T0, T0.plusSeconds(6000), 60); // 100 ticks

    private final NoOverlapFamily family = new NoOverlapFamily();

    private static Constraint constraint(IEnumEnforcement.Types e, ConstraintScope scope) {
        return new Constraint("no", "no_overlap", e, scope, Map.of(), null, 2, 0);
    }

    private static Constraint byPrefix(IEnumEnforcement.Types e) {
        return constraint(e, ConstraintScope.all());
    }

    private static ValidationContext validation(Constraint c) {
        ValidationContext ctx = mock(ValidationContext.class);
        when(ctx.problem()).thenReturn(new PlanningProblem(1L, "t", "UTC", H, List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(c), List.of(), null, null));
        return ctx;
    }

    @Test
    void identity() {
        assertEquals("no_overlap", family.type());
        assertEquals(NoOverlapParams.class, family.paramsType());
        assertEquals(4, family.supportedEnforcements().size());
    }

    @Test
    void validateRequiresPrefixForAllAndTagForTag() {
        Constraint all = byPrefix(IEnumEnforcement.Types.STRICTLY_REQUIRED);
        ValidationContext ctx = validation(all);
        family.validate(ctx, all, new NoOverlapParams(null, null));
        verify(ctx).error(eq("PARAMS_INVALID"), eq("constraints[0].params.groupByTagPrefix"), anyString());

        Constraint tag = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED, new ConstraintScope("tag", "class-7A"));
        ValidationContext ctx2 = validation(tag);
        family.validate(ctx2, tag, new NoOverlapParams("class-", 0));
        verify(ctx2).error(eq("PARAMS_INVALID"), eq("constraints[0].params.groupByTagPrefix"), anyString());

        Constraint noValue = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED, new ConstraintScope("tag", null));
        ValidationContext ctx3 = validation(noValue);
        family.validate(ctx3, noValue, new NoOverlapParams(null, null));
        verify(ctx3).error(eq("INVALID_SCOPE"), anyString(), anyString());

        Constraint wrongKind = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED, new ConstraintScope("resource", "r"));
        ValidationContext ctx4 = validation(wrongKind);
        family.validate(ctx4, wrongKind, new NoOverlapParams("x", null));
        verify(ctx4).error(eq("INVALID_SCOPE"), anyString(), anyString());
    }

    @Test
    void validateRejectsNegativeAndOversizedGap() {
        Constraint all = byPrefix(IEnumEnforcement.Types.STRICTLY_REQUIRED);
        ValidationContext ctx = validation(all);
        family.validate(ctx, all, new NoOverlapParams("class-", -1));
        verify(ctx).error(eq("PARAMS_INVALID"), eq("constraints[0].params.minGapTicks"), anyString());
        ValidationContext ctx2 = validation(all);
        family.validate(ctx2, all, new NoOverlapParams("class-", 101));
        verify(ctx2).error(eq("PARAMS_INVALID"), eq("constraints[0].params.minGapTicks"), anyString());
        ValidationContext ctx3 = validation(all);
        family.validate(ctx3, all, new NoOverlapParams("class-", 5));
        verify(ctx3, never()).error(anyString(), anyString(), anyString());
    }

    // ------------------------------------------------------------------ index fake: 4 tasks, one group {0,1,2,3}

    /** windows[t] = {est, lst, duration}. */
    private static CandidateIndexView index(int[][] windows) {
        CandidateIndexView idx = mock(CandidateIndexView.class);
        int[] group = new int[windows.length];
        for (int t = 0; t < group.length; t++) {
            group[t] = t;
        }
        when(idx.groupTasksByTagPrefix("class-")).thenReturn(new int[][]{group, new int[]{0}});
        when(idx.resolveTasks(any())).thenReturn(group);
        when(idx.estStart(anyInt())).thenAnswer(i -> windows[i.getArgument(0, Integer.class)][0]);
        when(idx.lstStart(anyInt())).thenAnswer(i -> windows[i.getArgument(0, Integer.class)][1]);
        when(idx.candidateBegin(anyInt())).thenAnswer(i -> i.getArgument(0, Integer.class));
        when(idx.candidateEnd(anyInt())).thenAnswer(i -> i.getArgument(0, Integer.class) + 1);
        when(idx.candidateDuration(anyInt())).thenAnswer(i -> windows[i.getArgument(0, Integer.class)][2]);
        when(idx.duration(anyInt())).thenAnswer(i -> windows[i.getArgument(0, Integer.class)][2]);
        when(idx.taskId(anyInt())).thenAnswer(i -> "t" + i.getArgument(0));
        return idx;
    }

    @Test
    void groupsDropSingletonsAndFollowTheScopeKind() {
        CandidateIndexView idx = index(new int[][]{{0, 10, 5}, {0, 10, 5}});
        int[][] byPrefix = NoOverlapFamily.groups(idx, byPrefix(IEnumEnforcement.Types.STRICTLY_REQUIRED), new NoOverlapParams("class-", null));
        assertEquals(1, byPrefix.length);
        assertEquals(2, byPrefix[0].length);
        int[][] byTag = NoOverlapFamily.groups(idx, constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED, new ConstraintScope("tag", "x")),
                new NoOverlapParams(null, null));
        assertEquals(1, byTag.length);
    }

    @Test
    void conflictPairsSkipPairsTheDomainsAlreadySeparateAndZeroSizeTasks() {
        // t0 [0..0]+5 ends <= 5, t1 starts >= 20: separated; t2 free; t3 zero duration
        CandidateIndexView idx = index(new int[][]{{0, 0, 5}, {20, 30, 5}, {0, 50, 5}, {0, 50, 0}});
        assertEquals(2, NoOverlapFamily.countConflictPairs(idx, new int[]{0, 1, 2, 3}, 0)); // (0,2) and (1,2)
        assertEquals(6, NoOverlapFamily.countConflictPairs(idx, new int[]{0, 1, 2, 3}, 20)); // gap 20 re-couples (0,1) and t3 is no longer zero size
        assertTrue(NoOverlapFamily.zeroSize(idx, 3, 0));
        assertFalse(NoOverlapFamily.zeroSize(idx, 3, 1));
    }

    @Test
    void estimateHardUsesGlobalConstraintsAndSoftUsesPairLiterals() {
        CandidateIndexView idx = index(new int[][]{{0, 50, 5}, {0, 50, 5}, {0, 50, 5}});
        NoOverlapParams p = new NoOverlapParams("class-", 0);
        ModelCost hard = family.estimate(idx, byPrefix(IEnumEnforcement.Types.STRICTLY_REQUIRED), p);
        assertEquals(1, hard.constraints());
        assertEquals(0, hard.pairwise());
        ModelCost soft = family.estimate(idx, byPrefix(IEnumEnforcement.Types.SOFT), p);
        assertEquals(3, soft.pairwise());
        assertEquals(9, soft.constraints());
        assertEquals(9, soft.boolVars());
        ModelCost relaxable = family.estimate(idx, byPrefix(IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION), p);
        assertEquals(6, relaxable.boolVars());
    }

    // ------------------------------------------------------------------ checker

    private static CheckContext checkContext(CandidateIndexView idx, int[][] times, boolean[] selected) {
        SolutionView sol = mock(SolutionView.class);
        when(sol.selected(anyInt())).thenAnswer(i -> selected[i.getArgument(0, Integer.class)]);
        when(sol.start(anyInt())).thenAnswer(i -> times[i.getArgument(0, Integer.class)][0]);
        when(sol.end(anyInt())).thenAnswer(i -> times[i.getArgument(0, Integer.class)][1]);
        CheckContext ctx = mock(CheckContext.class);
        when(ctx.index()).thenReturn(idx);
        when(ctx.solution()).thenReturn(sol);
        return ctx;
    }

    private List<String> run(int gap, int[][] times, boolean[] selected) {
        CandidateIndexView idx = index(new int[][]{{0, 50, 5}, {0, 50, 5}, {0, 50, 5}, {0, 50, 5}});
        List<String> out = new ArrayList<>();
        family.checker().check(checkContext(idx, times, selected), byPrefix(IEnumEnforcement.Types.SOFT), new NoOverlapParams("class-", gap),
                (t, r, code, msg, m) -> out.add(t + ":" + m));
        return out;
    }

    @Test
    void checkerCountsEveryOverlappingPairOnce() {
        // [0,10) [5,15) [10,20) [30,40): overlapping pairs (0,1) and (1,2); (0,2) touch only
        List<String> out = run(0, new int[][]{{0, 10}, {5, 15}, {10, 20}, {30, 40}}, new boolean[]{true, true, true, true});
        assertEquals(List.of("t0:1", "t1:1"), out);
    }

    @Test
    void checkerHonoursTheMinimumGap() {
        // with gap 5 the touching pair (0,2) [0,10)/[10,20) and (2,3) no: 20+5 <= 30
        List<String> out = run(5, new int[][]{{0, 10}, {5, 15}, {10, 20}, {30, 40}}, new boolean[]{true, true, true, true});
        assertEquals(List.of("t0:1", "t0:1", "t1:1"), out); // pairs (0,1), (0,2), (1,2)
    }

    @Test
    void checkerIgnoresUnselectedAndZeroSizeTasksWithoutGap() {
        // t1 unselected, t3 zero size sitting inside t0
        List<String> out = run(0, new int[][]{{0, 10}, {2, 8}, {20, 30}, {5, 5}}, new boolean[]{true, false, true, true});
        assertTrue(out.isEmpty());
        // with a gap the zero-size task does occupy [5, 5+gap)
        List<String> withGap = run(2, new int[][]{{0, 10}, {2, 8}, {20, 30}, {5, 5}}, new boolean[]{true, false, true, true});
        assertEquals(List.of("t0:1"), withGap);
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
