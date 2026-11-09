package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.params.TimeWindowParams;
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

class TimeWindowFamilyTest {

    private static final Instant T0 = Instant.parse("2026-01-05T00:00:00Z");
    private static final Horizon H = new Horizon(T0, T0.plusSeconds(6000), 60); // 100 ticks

    private final TimeWindowFamily family = new TimeWindowFamily();

    private static Instant at(int tick) {
        return T0.plusSeconds(tick * 60L);
    }

    private static Constraint constraint(IEnumEnforcement.Types e) {
        return new Constraint("tw", "time_window", e, ConstraintScope.all(), Map.of(), null, 2, 0);
    }

    private static PlanningProblem problem(Constraint c) {
        return new PlanningProblem(1L, "t", "UTC", H, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(c), List.of(), null, null);
    }

    private static ValidationContext validation(Constraint c) {
        ValidationContext ctx = mock(ValidationContext.class);
        when(ctx.problem()).thenReturn(problem(c));
        when(ctx.tick(any(), anyString())).thenAnswer(i -> H.toTick(i.getArgument(0)));
        return ctx;
    }

    @Test
    void identity() {
        assertEquals("time_window", family.type());
        assertEquals(TimeWindowParams.class, family.paramsType());
        assertEquals(4, family.supportedEnforcements().size());
    }

    @Test
    void validateNeedsOneBoundAndConsistentPairs() {
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED);
        ValidationContext none = validation(c);
        family.validate(none, c, new TimeWindowParams(null, null, null, null));
        verify(none).error(eq("PARAMS_INVALID"), eq("constraints[0].params"), anyString());

        ValidationContext startPair = validation(c);
        family.validate(startPair, c, new TimeWindowParams(at(30), at(10), null, null));
        verify(startPair).error(eq("PARAMS_INVALID"), eq("constraints[0].params.latestStart"), anyString());

        ValidationContext endPair = validation(c);
        family.validate(endPair, c, new TimeWindowParams(null, null, at(30), at(10)));
        verify(endPair).error(eq("PARAMS_INVALID"), eq("constraints[0].params.latestEnd"), anyString());

        ValidationContext ok = validation(c);
        family.validate(ok, c, new TimeWindowParams(at(10), at(30), at(20), at(60)));
        verify(ok, never()).error(anyString(), anyString(), anyString());
    }

    @Test
    void boundsAreTicksWithNoneForAbsentValues() {
        assertArrayEquals(new int[]{10, TimeWindowFamily.NONE, TimeWindowFamily.NONE, 60},
                TimeWindowFamily.bounds(H, new TimeWindowParams(at(10), null, null, at(60))));
    }

    @Test
    void violationMeasuresTicksOutsideEachBound() {
        assertEquals(5, TimeWindowFamily.violation(TimeWindowFamily.EARLIEST_START, 10, 5, 15));
        assertEquals(0, TimeWindowFamily.violation(TimeWindowFamily.EARLIEST_START, 10, 10, 15));
        assertEquals(7, TimeWindowFamily.violation(TimeWindowFamily.LATEST_START, 10, 17, 25));
        assertEquals(3, TimeWindowFamily.violation(TimeWindowFamily.EARLIEST_END, 20, 5, 17));
        assertEquals(4, TimeWindowFamily.violation(TimeWindowFamily.LATEST_END, 20, 10, 24));
        assertEquals(0, TimeWindowFamily.violation(TimeWindowFamily.LATEST_END, 20, 10, 20));
    }

    @Test
    void estimateIsZeroForHardAndPerBoundPerTaskOtherwise() {
        CandidateIndexView idx = mock(CandidateIndexView.class);
        when(idx.resolveTasks(any())).thenReturn(new int[]{0, 1, 2});
        TimeWindowParams p = new TimeWindowParams(at(1), null, null, at(50));
        assertEquals(ModelCost.ZERO, family.estimate(idx, constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED), p));
        ModelCost soft = family.estimate(idx, constraint(IEnumEnforcement.Types.SOFT), p);
        assertEquals(6, soft.constraints());
        assertEquals(6, soft.intVars());
        ModelCost relaxable = family.estimate(idx, constraint(IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION), p);
        assertEquals(6, relaxable.constraints());
        assertEquals(0, relaxable.intVars());
    }

    private PreprocessContext preprocessContext(int[] durations) {
        CandidateIndexBuilder.Mutable m = mock(CandidateIndexBuilder.Mutable.class);
        when(m.candidateBegin(0)).thenReturn(0);
        when(m.candidateEnd(0)).thenReturn(durations.length);
        when(m.candidateDuration(anyInt())).thenAnswer(i -> durations[i.getArgument(0, Integer.class)]);
        PreprocessContext ctx = mock(PreprocessContext.class);
        when(ctx.candidates()).thenReturn(m);
        when(ctx.horizon()).thenReturn(H);
        when(ctx.resolveTasks(any())).thenReturn(new int[]{0});
        return ctx;
    }

    @Test
    void preprocessRestrictsTheStartDomainFromAllFourBounds() {
        PreprocessContext ctx = preprocessContext(new int[]{10});
        family.preprocess(ctx, constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED), new TimeWindowParams(at(5), at(70), at(30), at(60)));
        // lo = max(5, 30-10) = 20 ; hi = min(70, 60-10) = 50
        verify(ctx).restrictStart(0, 20, 50);
    }

    @Test
    void preprocessUsesTheWeakestDurationForEndBounds() {
        PreprocessContext ctx = preprocessContext(new int[]{4, 10});
        family.preprocess(ctx, constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED), new TimeWindowParams(null, null, at(30), at(60)));
        // lo = 30 - maxDur(10) = 20 ; hi = 60 - minDur(4) = 56
        verify(ctx).restrictStart(0, 20, 56);
    }

    @Test
    void preprocessDoesNothingWhenNoBoundTightensTheDomain() {
        PreprocessContext ctx = preprocessContext(new int[]{10});
        family.preprocess(ctx, constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED), new TimeWindowParams(at(0), at(100), null, null));
        verify(ctx, never()).restrictStart(anyInt(), anyInt(), anyInt());
    }

    // ------------------------------------------------------------------ checker

    private List<String> check(TimeWindowParams p, int[][] times) {
        SolutionView sol = mock(SolutionView.class);
        when(sol.selected(anyInt())).thenAnswer(i -> times[i.getArgument(0, Integer.class)][0] >= 0);
        when(sol.start(anyInt())).thenAnswer(i -> times[i.getArgument(0, Integer.class)][0]);
        when(sol.end(anyInt())).thenAnswer(i -> times[i.getArgument(0, Integer.class)][1]);
        CandidateIndexView idx = mock(CandidateIndexView.class);
        when(idx.resolveTasks(any())).thenReturn(new int[]{0, 1, 2});
        when(idx.taskId(anyInt())).thenAnswer(i -> "t" + i.getArgument(0));
        CheckContext ctx = mock(CheckContext.class);
        when(ctx.problem()).thenReturn(problem(constraint(IEnumEnforcement.Types.SOFT)));
        when(ctx.index()).thenReturn(idx);
        when(ctx.solution()).thenReturn(sol);
        List<String> out = new ArrayList<>();
        family.checker().check(ctx, constraint(IEnumEnforcement.Types.SOFT), p, (t, r, code, msg, m) -> out.add(t + ":" + code + ":" + m));
        return out;
    }

    @Test
    void checkerReportsTicksPerViolatedBoundSoTheTotalEqualsTheModelMeasure() {
        TimeWindowParams p = new TimeWindowParams(at(10), at(20), at(15), at(40));
        // t0 [5,12): early start by 5 ; t1 [25,45): late start by 5 and late end by 5 ; t2 [10,30): fine
        List<String> out = check(p, new int[][]{{5, 12}, {25, 45}, {10, 30}});
        assertEquals(List.of("t0:EARLIEST_START_VIOLATED:5", "t0:EARLIEST_END_VIOLATED:3", "t1:LATEST_START_VIOLATED:5",
                "t1:LATEST_END_VIOLATED:5"), out);
    }

    @Test
    void checkerIgnoresUnselectedTasks() {
        List<String> out = check(new TimeWindowParams(at(10), null, null, null), new int[][]{{-1, -1}, {-1, -1}, {-1, -1}});
        assertTrue(out.isEmpty());
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
