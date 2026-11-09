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
import java.util.Map;

import static eu.isygoit.solver.family.FamilyKit.map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaxTasksPerResourceFamilyTest {

    private final MaxTasksPerResourceFamily family = new MaxTasksPerResourceFamily();

    private PlanningProblem problem(Constraint c) {
        return FamilyKit.problem(FamilyKit.horizon(72, 3600), "UTC",
                List.of(FamilyKit.resource("ana", 1), FamilyKit.resource("ben", 1)),
                List.of(FamilyKit.task("t1", 4), FamilyKit.task("t2", 4), FamilyKit.task("t3", 4)), List.of(c));
    }

    private Constraint hard(Map<String, Object> params) {
        return FamilyKit.hard("cap", "max_tasks_per_resource", params);
    }

    private Sink check(Constraint c, boolean benHasCandidates) {
        PlanningProblem p = problem(c);
        View v = View.of(p);
        for (String t : new String[]{"t1", "t2", "t3"}) {
            v.candidate(t, "ana");
            if (benHasCandidates) {
                v.candidate(t, "ben");
            }
        }
        Solution s = new Solution(v).assign("t1", 0, 4, "ana").assign("t2", 6, 10, "ana").assign("t3", 30, 34, "ana");
        return FamilyKit.run(family, p, v, s, c);
    }

    @Test
    void identity() {
        assertEquals("max_tasks_per_resource", family.type());
        assertEquals(ScopeKind.RESOURCES, family.scopeKind());
        assertEquals(List.of("max", "min", "period"), family.paramsHint());
    }

    @Test
    void validate() {
        Constraint none = hard(map());
        Validation v = new Validation(View.of(problem(none)));
        family.validate(v, none, FamilyKit.bind(family, none));
        assertTrue(v.hasError("PARAMS_INVALID"));

        Constraint badPeriod = hard(map("max", 2, "period", map("unit", "TICKS", "ticks", 0)));
        Validation w = new Validation(View.of(problem(badPeriod)));
        family.validate(w, badPeriod, FamilyKit.bind(family, badPeriod));
        assertTrue(w.hasError("PARAMS_INVALID"));

        Constraint good = hard(map("max", 2));
        Validation ok = new Validation(View.of(problem(good)));
        family.validate(ok, good, FamilyKit.bind(family, good));
        assertTrue(ok.errors.isEmpty());
    }

    @Test
    void estimateWholeHorizonIsSmall() {
        Constraint c = hard(map("max", 2));
        PlanningProblem p = problem(c);
        View v = View.of(p).allCandidates();
        ModelCost cost = family.estimate(v, c, FamilyKit.bind(family, c));
        assertEquals(6, cost.boolVars());   // 3 tasks x 2 resources, one bucket
    }

    @Test
    void maxOverTheWholeHorizonReportsTheExcess() {
        Sink sink = check(hard(map("max", 2)), false);
        assertEquals(1, sink.count());
        assertEquals(1, sink.measure());
        assertEquals("ana", sink.violations.get(0).resourceId());
        assertEquals(MaxTasksPerResourceFamily.CODE_MAX, sink.violations.get(0).code());
        assertEquals(0, check(hard(map("max", 3)), false).count());
    }

    @Test
    void minAppliesOnlyToResourcesThatHaveCandidates() {
        // ana has 3 tasks (min 2 fine); ben has candidates and 0 tasks -> deficit 2
        Sink with = check(hard(map("min", 2)), true);
        assertEquals(1, with.count());
        assertEquals(2, with.measure());
        assertEquals("ben", with.violations.get(0).resourceId());
        assertEquals(MaxTasksPerResourceFamily.CODE_MIN, with.violations.get(0).code());
        // ben has no candidate at all: nothing to report
        assertEquals(0, check(hard(map("min", 2)), false).count());
    }

    @Test
    void perPeriodCountsTheTaskStartPeriod() {
        Sink sink = check(hard(map("max", 1, "period", map("unit", "DAY"))), false);
        assertEquals(1, sink.count());    // day 0 holds t1 and t2, day 1 holds t3
        assertEquals(1, sink.measure());
    }
}
