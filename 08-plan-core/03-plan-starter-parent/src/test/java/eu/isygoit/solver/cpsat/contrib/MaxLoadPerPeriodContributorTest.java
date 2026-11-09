package eu.isygoit.solver.cpsat.contrib;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.family.FamilyKit;
import eu.isygoit.solver.family.MaxLoadPerPeriodFamily;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static eu.isygoit.solver.family.FamilyKit.map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaxLoadPerPeriodContributorTest {

    private final MaxLoadPerPeriodFamily family = new MaxLoadPerPeriodFamily();
    private final MaxLoadPerPeriodContributor contributor = new MaxLoadPerPeriodContributor();

    @BeforeEach
    void requiresNatives() {
        Assumptions.assumeTrue(ContribFixture.nativeAvailable(), "OR-Tools natives not available");
    }

    private Constraint constraint(IEnumEnforcement.Types enforcement, Map<String, Object> extra) {
        Map<String, Object> params = map("period", map("unit", "DAY"));
        params.putAll(extra);
        return FamilyKit.constraint("load", "max_load_per_period", enforcement, null,
                enforcement == IEnumEnforcement.Types.SOFT ? 1 : 0, params);
    }

    /** {@code days} days of hourly ticks, tasks of the given durations free to start anywhere, the given resources. */
    private ContribFixture fixture(Constraint c, int days, int[] durations, String... resources) {
        List<Resource> rs = new ArrayList<>();
        for (String r : resources) {
            rs.add(FamilyKit.resource(r, 1));
        }
        List<eu.isygoit.dto.request.Task> ts = new ArrayList<>();
        for (int i = 0; i < durations.length; i++) {
            ts.add(FamilyKit.task("t" + i, durations[i]));
        }
        PlanningProblem p = FamilyKit.problem(FamilyKit.horizon(24 * days, 3600), "UTC", rs, ts, List.of(c));
        return ContribFixture.of(FamilyKit.View.of(p).allCandidates());
    }

    @Test
    void identityAndSupport() {
        assertEquals("max_load_per_period", contributor.type());
        assertEquals(EnforcementSupport.ALL, contributor.support());
    }

    @Test
    void hardMaxForcesTheLoadToSpreadOverDays() {
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED, map("max", 8));
        ContribFixture two = fixture(c, 2, new int[]{8, 8}, "ana");
        contributor.apply(two.ctx, two.compile(family, c), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, two));
        ContribFixture.Solved s = two.solve(null);
        assertTrue(s.solved());
        assertTrue(Math.floorDiv(s.startOf("t0"), 24) != Math.floorDiv(s.startOf("t1"), 24), "tasks must start on different days");
        assertEquals(0, FamilyKit.run(family, two.problem, two.view, s.toSolution(), c).count());

        ContribFixture one = fixture(c, 1, new int[]{8, 8}, "ana");
        contributor.apply(one.ctx, one.compile(family, c), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, one));
        assertTrue(one.solve(null).infeasible());
    }

    @Test
    void softExcessIsMeasuredInTicksAndMatchesTheChecker() {
        Constraint c = constraint(IEnumEnforcement.Types.SOFT, map("max", 8));
        ContribFixture f = fixture(c, 1, new int[]{8, 8, 8}, "ana");
        ContribFixture.FakeEnforcer e = new ContribFixture.FakeEnforcer(Enforcer.Mode.SOFT, f);
        contributor.apply(f.ctx, f.compile(family, c), e);
        ContribFixture.Solved s = f.solve(e.penalty());
        assertEquals(16, Math.round(s.objective()));
        assertEquals(16, FamilyKit.run(family, f.problem, f.view, s.toSolution(), c).measure());
    }

    @Test
    void countMetricCountsTasksNotTicks() {
        Constraint c = constraint(IEnumEnforcement.Types.SOFT, map("metric", "COUNT", "max", 1));
        ContribFixture f = fixture(c, 1, new int[]{8, 8, 2}, "ana");
        ContribFixture.FakeEnforcer e = new ContribFixture.FakeEnforcer(Enforcer.Mode.SOFT, f);
        contributor.apply(f.ctx, f.compile(family, c), e);
        ContribFixture.Solved s = f.solve(e.penalty());
        assertEquals(2, Math.round(s.objective()));
        assertEquals(2, FamilyKit.run(family, f.problem, f.view, s.toSolution(), c).measure());
    }

    @Test
    void minDeficitSoftAndHard() {
        Constraint soft = constraint(IEnumEnforcement.Types.SOFT, map("min", 8));
        ContribFixture f = fixture(soft, 2, new int[]{8}, "ana");
        ContribFixture.FakeEnforcer e = new ContribFixture.FakeEnforcer(Enforcer.Mode.SOFT, f);
        contributor.apply(f.ctx, f.compile(family, soft), e);
        ContribFixture.Solved s = f.solve(e.penalty());
        assertEquals(8, Math.round(s.objective()));          // one of the two days is empty
        assertEquals(8, FamilyKit.run(family, f.problem, f.view, s.toSolution(), soft).measure());

        Constraint hard = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED, map("min", 8));
        ContribFixture h = fixture(hard, 2, new int[]{8}, "ana");
        contributor.apply(h.ctx, h.compile(family, hard), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, h));
        assertTrue(h.solve(null).infeasible());

        ContribFixture enough = fixture(hard, 2, new int[]{8, 8}, "ana");
        contributor.apply(enough.ctx, enough.compile(family, hard), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, enough));
        assertTrue(enough.solve(null).solved());
    }

    @Test
    void relaxableAndExplainFollowTheSharedLiteral() {
        Constraint c = constraint(IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION, map("max", 8));
        ContribFixture on = fixture(c, 1, new int[]{8, 8}, "ana");
        ContribFixture.FakeEnforcer active = new ContribFixture.FakeEnforcer(Enforcer.Mode.RELAXABLE, on);
        contributor.apply(on.ctx, on.compile(family, c), active);
        on.model.addEquality(active.shared(), 1);
        assertTrue(on.solve(null).infeasible());

        ContribFixture off = fixture(c, 1, new int[]{8, 8}, "ana");
        ContribFixture.FakeEnforcer dropped = new ContribFixture.FakeEnforcer(Enforcer.Mode.RELAXABLE, off);
        contributor.apply(off.ctx, off.compile(family, c), dropped);
        off.model.addEquality(dropped.shared(), 0);
        assertTrue(off.solve(null).solved());

        Constraint hard = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED, map("max", 8));
        ContribFixture explain = fixture(hard, 1, new int[]{8, 8}, "ana");
        explain.explain = true;
        ContribFixture.FakeEnforcer assumption = new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, explain);
        contributor.apply(explain.ctx, explain.compile(family, hard), assumption);
        explain.model.addEquality(assumption.shared(), 1);
        assertTrue(explain.solve(null).infeasible());
    }

    @Test
    void aBoundThatCannotBeExceededIsHarmless() {
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED, map("max", 100));
        ContribFixture f = fixture(c, 2, new int[]{8, 8}, "ana");
        contributor.apply(f.ctx, f.compile(family, c), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, f));
        assertTrue(f.solve(null).solved());
    }
}
