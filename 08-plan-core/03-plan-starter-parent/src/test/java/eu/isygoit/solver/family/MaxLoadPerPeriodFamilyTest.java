package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.solver.family.FamilyKit.Sink;
import eu.isygoit.solver.family.FamilyKit.Solution;
import eu.isygoit.solver.family.FamilyKit.Validation;
import eu.isygoit.solver.family.FamilyKit.View;
import eu.isygoit.solver.spi.ModelCost;
import eu.isygoit.solver.spi.ScopeKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static eu.isygoit.solver.family.FamilyKit.map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaxLoadPerPeriodFamilyTest {

    private final MaxLoadPerPeriodFamily family = new MaxLoadPerPeriodFamily();

    private PlanningProblem problem(Constraint c) {
        return FamilyKit.problem(FamilyKit.horizon(72, 3600), "UTC", List.of(FamilyKit.resource("ana", 1)),
                List.of(FamilyKit.task("s1", 8), FamilyKit.task("s2", 8), FamilyKit.task("s3", 4)), List.of(c));
    }

    private Constraint constraint(Object metric, Integer max, Integer min) {
        java.util.Map<String, Object> params = map("period", map("unit", "DAY"));
        if (metric != null) {
            params.put("metric", metric);
        }
        if (max != null) {
            params.put("max", max);
        }
        if (min != null) {
            params.put("min", min);
        }
        return FamilyKit.hard("load", "max_load_per_period", params);
    }

    /** s1 [8,16) and s2 [18,26)... both start on day 0; s3 starts on day 1. */
    private Sink check(Constraint c, boolean s3Selected) {
        PlanningProblem p = problem(c);
        View v = View.of(p).allCandidates();
        Solution s = new Solution(v).assign("s1", 8, 16, "ana").assign("s2", 16, 24, "ana");
        if (s3Selected) {
            s.assign("s3", 30, 34, "ana");
        }
        return FamilyKit.run(family, p, v, s, c);
    }

    @Test
    void identity() {
        assertEquals("max_load_per_period", family.type());
        assertEquals(ScopeKind.RESOURCES, family.scopeKind());
        assertEquals("plan.constraint.max_load_per_period.description", family.descriptionKey());
        assertEquals(List.of("period", "metric", "max", "min"), family.paramsHint());
    }

    @Test
    void validateBounds() {
        Constraint none = constraint(null, null, null);
        Validation v = new Validation(View.of(problem(none)));
        family.validate(v, none, FamilyKit.bind(family, none));
        assertTrue(v.hasError("PARAMS_INVALID"));

        Constraint inverted = constraint(null, 4, 8);
        Validation w = new Validation(View.of(problem(inverted)));
        family.validate(w, inverted, FamilyKit.bind(family, inverted));
        assertTrue(w.hasError("PARAMS_INVALID"));

        Constraint good = constraint("COUNT", 2, 0);
        Validation ok = new Validation(View.of(problem(good)));
        family.validate(ok, good, FamilyKit.bind(family, good));
        assertTrue(ok.errors.isEmpty());

        Constraint noPeriod = FamilyKit.hard("load", "max_load_per_period", map("max", 3));
        Validation x = new Validation(View.of(problem(noPeriod)));
        family.validate(x, noPeriod, FamilyKit.bind(family, noPeriod));
        assertTrue(x.hasError("MISSING_REQUIRED_FIELD"));
    }

    @Test
    void estimateIsPositiveWithCandidates() {
        Constraint c = constraint(null, 8, null);
        PlanningProblem p = problem(c);
        ModelCost cost = family.estimate(View.of(p).allCandidates(), c, FamilyKit.bind(family, c));
        assertTrue(cost.boolVars() > 0);
        assertEquals(ModelCost.ZERO, family.estimate(View.of(p), c, FamilyKit.bind(family, c)));
    }

    @Test
    void ticksExcessIsTheSoftUnit() {
        Sink sink = check(constraint(null, 8, null), true);
        assertEquals(1, sink.count());                       // day 0: 16 ticks, day 1: 4 ticks
        assertEquals(8, sink.measure());
        assertEquals("ana", sink.violations.get(0).resourceId());
        assertEquals(MaxLoadPerPeriodFamily.CODE_MAX, sink.violations.get(0).code());
    }

    @Test
    void countMetricCountsTasks() {
        Sink sink = check(constraint("COUNT", 1, null), true);
        assertEquals(1, sink.count());
        assertEquals(1, sink.measure());
        assertEquals(0, check(constraint("COUNT", 2, null), true).count());
    }

    @Test
    void minBoundReportsTheDeficitOnReachablePeriods() {
        // day 2 is reachable by s3 (window 0..68) but has no task; min 4 ticks on day 0..2
        Sink sink = check(constraint(null, null, 4), true);
        assertEquals(1, sink.count());
        assertEquals(4, sink.measure());
        assertEquals(MaxLoadPerPeriodFamily.CODE_MIN, sink.violations.get(0).code());
        // without s3 on day 1 two periods are short
        assertEquals(2, check(constraint(null, null, 4), false).count());
    }

    @Test
    void periodsOfDifferentResourcesAreIndependent() {
        Constraint c = constraint(null, 8, null);
        PlanningProblem p = FamilyKit.problem(FamilyKit.horizon(72, 3600), "UTC",
                List.of(FamilyKit.resource("ana", 1), FamilyKit.resource("ben", 1)),
                List.of(FamilyKit.task("s1", 8), FamilyKit.task("s2", 8)), List.of(c));
        View v = View.of(p).allCandidates();
        Solution s = new Solution(v).assign("s1", 8, 16, "ana").assign("s2", 16, 24, "ben");
        assertEquals(0, FamilyKit.run(family, p, v, s, c).count());
    }
}
