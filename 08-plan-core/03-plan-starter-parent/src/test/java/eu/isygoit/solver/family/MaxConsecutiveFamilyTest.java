package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.enums.IEnumEnforcement;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaxConsecutiveFamilyTest {

    private final MaxConsecutiveFamily family = new MaxConsecutiveFamily();

    /** 4 days of hourly ticks, one nurse, one 8-tick shift per day starting at 08:00. */
    private PlanningProblem problem(Constraint c) {
        return FamilyKit.problem(FamilyKit.horizon(96, 3600), "UTC", List.of(FamilyKit.resource("ana", 1)),
                List.of(FamilyKit.task("d0", 8), FamilyKit.task("d1", 8), FamilyKit.task("d2", 8), FamilyKit.task("d3", 8)),
                List.of(c));
    }

    private Constraint constraint(int max) {
        return FamilyKit.hard("c", "max_consecutive", map("period", map("unit", "DAY"), "max", max));
    }

    private View view(PlanningProblem p) {
        View v = View.of(p).allCandidates();
        for (int d = 0; d < 4; d++) {
            v.pinned("d" + d, 24 * d + 8);
        }
        return v;
    }

    private Sink check(int max, String... workedTasks) {
        Constraint c = constraint(max);
        PlanningProblem p = problem(c);
        View v = view(p);
        Solution s = new Solution(v);
        for (String t : workedTasks) {
            int d = Integer.parseInt(t.substring(1));
            s.assign(t, 24 * d + 8, 24 * d + 16, "ana");
        }
        return FamilyKit.run(family, p, v, s, c);
    }

    @Test
    void identity() {
        assertEquals("max_consecutive", family.type());
        assertEquals(eu.isygoit.dto.request.params.MaxConsecutiveParams.class, family.paramsType());
        assertEquals(ScopeKind.RESOURCES, family.scopeKind());
        assertTrue(family.supportedEnforcements().contains(IEnumEnforcement.Types.SOFT));
        assertEquals("plan.constraint.max_consecutive.description", family.descriptionKey());
    }

    @Test
    void validateAcceptsGoodParamsAndRejectsBadOnes() {
        Constraint good = constraint(2);
        Validation ok = new Validation(View.of(problem(good)));
        family.validate(ok, good, FamilyKit.bind(family, good));
        assertTrue(ok.errors.isEmpty());

        Constraint noPeriod = FamilyKit.hard("c", "max_consecutive", map("max", 2));
        Validation v1 = new Validation(View.of(problem(noPeriod)));
        family.validate(v1, noPeriod, FamilyKit.bind(family, noPeriod));
        assertTrue(v1.hasError("MISSING_REQUIRED_FIELD"));

        Constraint zero = FamilyKit.hard("c", "max_consecutive", map("period", map("unit", "DAY"), "max", 0));
        Validation v2 = new Validation(View.of(problem(zero)));
        family.validate(v2, zero, FamilyKit.bind(family, zero));
        assertTrue(v2.hasError("PARAMS_INVALID"));

        Constraint ticks = FamilyKit.hard("c", "max_consecutive", map("period", map("unit", "TICKS"), "max", 2));
        Validation v3 = new Validation(View.of(problem(ticks)));
        family.validate(v3, ticks, FamilyKit.bind(family, ticks));
        assertTrue(v3.errors.stream().anyMatch(e -> e.contains("period.ticks")));
    }

    @Test
    void estimateGrowsWithReachableBuckets() {
        Constraint c = constraint(2);
        PlanningProblem p = problem(c);
        ModelCost cost = family.estimate(view(p), c, FamilyKit.bind(family, c));
        assertTrue(cost.boolVars() > 0);
        assertTrue(cost.constraints() > 0);
        assertEquals(ModelCost.ZERO, family.estimate(View.of(p), c, FamilyKit.bind(family, c)));
    }

    @Test
    void checkerCountsOneViolationPerWindowOfMaxPlusOne() {
        assertEquals(2, check(2, "d0", "d1", "d2", "d3").count());   // windows [0..2] and [1..3]
        assertEquals(1, check(3, "d0", "d1", "d2", "d3").count());
        assertEquals(0, check(4, "d0", "d1", "d2", "d3").count());
        assertEquals(1, check(2, "d0", "d1", "d2").count());
    }

    @Test
    void gapBreaksTheRun() {
        assertEquals(0, check(2, "d0", "d1", "d3").count());
        assertEquals(0, check(1, "d0", "d2").count());
        assertEquals(1, check(1, "d0", "d1", "d3").count());
    }

    @Test
    void violationsCarryTheResource() {
        Sink sink = check(1, "d0", "d1");
        assertEquals(1, sink.count());
        assertEquals("ana", sink.violations.get(0).resourceId());
        assertEquals(MaxConsecutiveFamily.CODE, sink.violations.get(0).code());
        assertFalse(sink.violations.get(0).message().isEmpty());
    }
}
