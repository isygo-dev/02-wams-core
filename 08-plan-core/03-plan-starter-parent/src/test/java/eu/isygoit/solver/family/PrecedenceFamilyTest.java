package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.PrecedenceRef;
import eu.isygoit.dto.request.params.PrecedenceParams;
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

class PrecedenceFamilyTest {

    private static final Instant T0 = Instant.parse("2026-01-05T00:00:00Z");
    private static final Horizon H = new Horizon(T0, T0.plusSeconds(6000), 60); // 100 ticks

    private final PrecedenceFamily family = new PrecedenceFamily();

    private static final ConstraintScope A = new ConstraintScope("tag", "a");
    private static final ConstraintScope B = new ConstraintScope("tag", "b");

    private static Constraint constraint(IEnumEnforcement.Types e) {
        return new Constraint("prec", "precedence", e, ConstraintScope.all(), Map.of(), null, 4, 0);
    }

    private static PrecedenceParams params(ConstraintScope from, ConstraintScope to, PrecedenceRef.Type type, Integer minLag, Integer maxLag,
                                           PrecedenceParams.PairingMode pairing) {
        return new PrecedenceParams(from, to, type, minLag, maxLag, pairing);
    }

    /** tag a -> tasks {0,1}; tag b -> tasks {2,3}; "task" scope -> that single task. */
    private static CandidateIndexView index() {
        CandidateIndexView idx = mock(CandidateIndexView.class);
        when(idx.resolveTasks(any())).thenAnswer(i -> {
            ConstraintScope s = i.getArgument(0);
            if ("tag".equals(s.kind())) {
                return "a".equals(s.value()) ? new int[]{0, 1} : new int[]{2, 3};
            }
            if ("task".equals(s.kind())) {
                return new int[]{Integer.parseInt(s.value())};
            }
            return new int[]{0, 1, 2, 3};
        });
        when(idx.taskId(anyInt())).thenAnswer(i -> "t" + i.getArgument(0));
        return idx;
    }

    private static ValidationContext validation(Constraint c, CandidateIndexView idx) {
        ValidationContext ctx = mock(ValidationContext.class);
        when(ctx.problem()).thenReturn(new PlanningProblem(1L, "t", "UTC", H, List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(c), List.of(), null, null));
        when(ctx.index()).thenReturn(idx);
        when(ctx.hasTask(anyString())).thenAnswer(i -> !"ghost".equals(i.getArgument(0)));
        return ctx;
    }

    @Test
    void identity() {
        assertEquals("precedence", family.type());
        assertEquals(PrecedenceParams.class, family.paramsType());
        assertEquals(4, family.supportedEnforcements().size());
    }

    @Test
    void validateRejectsMissingSidesBadKindsLagsAndUnknownTask() {
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED);
        CandidateIndexView idx = index();
        ValidationContext ctx = validation(c, idx);
        family.validate(ctx, c, params(null, B, null, null, null, null));
        verify(ctx).error(eq("PARAMS_INVALID"), eq("constraints[0].params.from"), anyString());

        ValidationContext ctx2 = validation(c, idx);
        family.validate(ctx2, c, params(new ConstraintScope("resource", "r"), B, null, null, null, null));
        verify(ctx2).error(eq("INVALID_SCOPE"), eq("constraints[0].params.from.kind"), anyString());

        ValidationContext ctx3 = validation(c, idx);
        family.validate(ctx3, c, params(new ConstraintScope("task", "ghost"), B, null, null, null, null));
        verify(ctx3).error(eq("UNKNOWN_REFERENCE"), eq("constraints[0].params.from.value"), anyString());

        ValidationContext ctx4 = validation(c, idx);
        family.validate(ctx4, c, params(A, B, null, 10, 5, null));
        verify(ctx4).error(eq("INVALID_LAG"), eq("constraints[0].params.maxLag"), anyString());

        ValidationContext ctx5 = validation(c, idx);
        family.validate(ctx5, c, params(A, B, null, 500, null, null));
        verify(ctx5).error(eq("INVALID_LAG"), eq("constraints[0].params.minLag"), anyString());
    }

    @Test
    void validateByOrderNeedsEqualSidesAndNoSelfPair() {
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED);
        CandidateIndexView idx = index();
        ValidationContext ok = validation(c, idx);
        family.validate(ok, c, params(A, B, null, null, null, PrecedenceParams.PairingMode.BY_ORDER));
        verify(ok, never()).error(anyString(), anyString(), anyString());

        ValidationContext unequal = validation(c, idx);
        family.validate(unequal, c, params(A, new ConstraintScope("task", "2"), null, null, null, PrecedenceParams.PairingMode.BY_ORDER));
        verify(unequal).error(eq("PARAMS_INVALID"), eq("constraints[0].params.pairing"), anyString());

        ValidationContext self = validation(c, idx);
        family.validate(self, c, params(A, A, null, null, null, PrecedenceParams.PairingMode.BY_ORDER));
        verify(self).error(eq("PARAMS_INVALID"), eq("constraints[0].params.pairing"), anyString());
    }

    @Test
    void pairEnumerationAllPairsAndByOrder() {
        CandidateIndexView idx = index();
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED);
        List<String> pairs = new ArrayList<>();
        PrecedenceFamily.forEachPair(idx, c, params(A, B, null, null, null, null), (i, j) -> pairs.add(i + ">" + j));
        assertEquals(List.of("0>2", "0>3", "1>2", "1>3"), pairs);
        assertEquals(4, PrecedenceFamily.pairCount(idx, c, params(A, B, null, null, null, null)));

        List<String> ordered = new ArrayList<>();
        PrecedenceParams byOrder = params(A, B, null, null, null, PrecedenceParams.PairingMode.BY_ORDER);
        PrecedenceFamily.forEachPair(idx, c, byOrder, (i, j) -> ordered.add(i + ">" + j));
        assertEquals(List.of("0>2", "1>3"), ordered);
        assertEquals(2, PrecedenceFamily.pairCount(idx, c, byOrder));
    }

    @Test
    void selfPairsAreSkippedInAllPairs() {
        CandidateIndexView idx = index();
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED);
        PrecedenceParams p = params(A, A, null, null, null, null);
        List<String> pairs = new ArrayList<>();
        PrecedenceFamily.forEachPair(idx, c, p, (i, j) -> pairs.add(i + ">" + j));
        assertEquals(List.of("0>1", "1>0"), pairs);
        assertEquals(2, PrecedenceFamily.pairCount(idx, c, p));
    }

    @Test
    void constraintScopeFiltersBothSides() {
        CandidateIndexView idx = index();
        Constraint scoped = new Constraint("prec", "precedence", IEnumEnforcement.Types.STRICTLY_REQUIRED, new ConstraintScope("task", "2"),
                Map.of(), null, 1, 0);
        List<String> pairs = new ArrayList<>();
        PrecedenceFamily.forEachPair(idx, scoped, params(A, B, null, null, null, null), (i, j) -> pairs.add(i + ">" + j));
        assertTrue(pairs.isEmpty()); // from side {0,1} intersected with {2} is empty
    }

    @Test
    void estimateCountsPairsAndLagConstraints() {
        CandidateIndexView idx = index();
        ModelCost hard = family.estimate(idx, constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED), params(A, B, null, null, null, null));
        assertEquals(4, hard.constraints());
        assertEquals(4, hard.pairwise());
        assertEquals(0, hard.boolVars());
        ModelCost softMax = family.estimate(idx, constraint(IEnumEnforcement.Types.SOFT), params(A, B, null, 0, 9, null));
        assertEquals(8, softMax.constraints());
        assertEquals(4, softMax.boolVars());
    }

    @Test
    void violatedFollowsTheFourLinkTypesAndTheLags() {
        // i = [10, 20), j = [25, 40)
        assertFalse(PrecedenceFamily.violated(PrecedenceRef.Type.FINISH_START, 10, 20, 25, 40, 5, null));
        assertTrue(PrecedenceFamily.violated(PrecedenceRef.Type.FINISH_START, 10, 20, 25, 40, 6, null));
        assertTrue(PrecedenceFamily.violated(PrecedenceRef.Type.FINISH_START, 10, 20, 25, 40, 0, 4)); // max lag broken
        assertFalse(PrecedenceFamily.violated(PrecedenceRef.Type.START_START, 10, 20, 25, 40, 15, 15));
        assertFalse(PrecedenceFamily.violated(PrecedenceRef.Type.FINISH_FINISH, 10, 20, 25, 40, 20, null));
        assertTrue(PrecedenceFamily.violated(PrecedenceRef.Type.FINISH_FINISH, 10, 20, 25, 40, 21, null));
        assertFalse(PrecedenceFamily.violated(PrecedenceRef.Type.START_FINISH, 10, 20, 25, 40, 30, null));
        assertTrue(PrecedenceFamily.violated(PrecedenceRef.Type.START_FINISH, 10, 20, 25, 40, 31, null));
        // negative lag = lead
        assertFalse(PrecedenceFamily.violated(PrecedenceRef.Type.FINISH_START, 10, 20, 15, 30, -5, null));
        assertTrue(PrecedenceFamily.violated(PrecedenceRef.Type.FINISH_START, 10, 20, 14, 30, -5, null));
    }

    // ------------------------------------------------------------------ checker

    /** times[t] = {start, end}; start < 0 = unselected. */
    private List<String> check(PrecedenceParams p, int[][] times) {
        SolutionView sol = mock(SolutionView.class);
        when(sol.selected(anyInt())).thenAnswer(i -> times[i.getArgument(0, Integer.class)][0] >= 0);
        when(sol.start(anyInt())).thenAnswer(i -> times[i.getArgument(0, Integer.class)][0]);
        when(sol.end(anyInt())).thenAnswer(i -> times[i.getArgument(0, Integer.class)][1]);
        CheckContext ctx = mock(CheckContext.class);
        when(ctx.index()).thenReturn(index());
        when(ctx.solution()).thenReturn(sol);
        List<String> out = new ArrayList<>();
        family.checker().check(ctx, constraint(IEnumEnforcement.Types.SOFT), p, (t, r, code, msg, m) -> out.add(t + ":" + m));
        return out;
    }

    @Test
    void checkerCountsOneUnitPerViolatedPair() {
        // a = {0,1} must finish before b = {2,3} start; t0 [0,10) t1 [0,30) t2 [20,40) t3 [5,15)
        List<String> out = check(params(A, B, null, null, null, null), new int[][]{{0, 10}, {0, 30}, {20, 40}, {5, 15}});
        // pairs: 0>2 ok (10<=20), 0>3 violated (10>5), 1>2 violated (30>20), 1>3 violated
        assertEquals(List.of("t3:1", "t2:1", "t3:1"), out);
    }

    @Test
    void checkerCountsAMinAndMaxLagBreachOfTheSamePairOnce() {
        // pair (0, 2): FS, minLag 0, maxLag 5: j starts 20, i ends 10 -> gap 10 > 5 violated once
        List<String> out = check(params(new ConstraintScope("task", "0"), new ConstraintScope("task", "2"), null, 0, 5, null),
                new int[][]{{0, 10}, {0, 0}, {20, 40}, {0, 0}});
        assertEquals(List.of("t2:1"), out);
    }

    @Test
    void checkerSkipsPairsWithAnUnselectedEndpoint() {
        List<String> out = check(params(A, B, null, null, null, null), new int[][]{{0, 50}, {-1, -1}, {-1, -1}, {5, 15}});
        assertEquals(List.of("t3:1"), out); // only the pair (0, 3) has both endpoints selected and is violated
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
