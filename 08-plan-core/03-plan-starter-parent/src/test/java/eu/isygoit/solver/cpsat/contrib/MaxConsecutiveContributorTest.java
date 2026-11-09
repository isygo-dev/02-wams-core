package eu.isygoit.solver.cpsat.contrib;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.family.FamilyKit;
import eu.isygoit.solver.family.MaxConsecutiveFamily;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static eu.isygoit.solver.family.FamilyKit.map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaxConsecutiveContributorTest {

    private final MaxConsecutiveFamily family = new MaxConsecutiveFamily();
    private final MaxConsecutiveContributor contributor = new MaxConsecutiveContributor();

    @BeforeEach
    void requiresNatives() {
        Assumptions.assumeTrue(ContribFixture.nativeAvailable(), "OR-Tools natives not available");
    }

    private Constraint constraint(IEnumEnforcement.Types enforcement, int max) {
        return FamilyKit.constraint("c", "max_consecutive", enforcement, null, enforcement == IEnumEnforcement.Types.SOFT ? 1 : 0,
                map("period", map("unit", "DAY"), "max", max));
    }

    /** Four days (96 hourly ticks), one 8-tick task per day (free start inside its day), the given resources. */
    private ContribFixture fixture(Constraint c, String... resources) {
        List<eu.isygoit.dto.request.Resource> rs = new java.util.ArrayList<>();
        for (String r : resources) {
            rs.add(FamilyKit.resource(r, 1));
        }
        PlanningProblem p = FamilyKit.problem(FamilyKit.horizon(96, 3600), "UTC", rs,
                List.of(FamilyKit.task("d0", 8), FamilyKit.task("d1", 8), FamilyKit.task("d2", 8), FamilyKit.task("d3", 8)),
                List.of(c));
        FamilyKit.View v = FamilyKit.View.of(p).allCandidates();
        for (int d = 0; d < 4; d++) {
            v.window("d" + d, 24 * d, 24 * d + 16);
        }
        return ContribFixture.of(v);
    }

    @Test
    void identityAndSupport() {
        assertEquals("max_consecutive", contributor.type());
        assertEquals(EnforcementSupport.ALL, contributor.support());
    }

    @Test
    void hardOneResourceFourDaysInARowIsInfeasible() {
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED, 2);
        ContribFixture f = fixture(c, "ana");
        contributor.apply(f.ctx, f.compile(family, c), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, f));
        assertTrue(f.solve(null).infeasible());
    }

    @Test
    void hardTwoResourcesShareTheDaysAndTheCheckerAgrees() {
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED, 2);
        ContribFixture f = fixture(c, "ana", "ben");
        contributor.apply(f.ctx, f.compile(family, c), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, f));
        ContribFixture.Solved s = f.solve(null);
        assertTrue(s.solved());
        FamilyKit.Sink sink = FamilyKit.run(family, f.problem, f.view, s.toSolution(), c);
        assertEquals(0, sink.count());
    }

    @Test
    void softPenaltyEqualsTheCheckerUnitCount() {
        Constraint c = constraint(IEnumEnforcement.Types.SOFT, 2);
        ContribFixture f = fixture(c, "ana");
        ContribFixture.FakeEnforcer enforcer = new ContribFixture.FakeEnforcer(Enforcer.Mode.SOFT, f);
        contributor.apply(f.ctx, f.compile(family, c), enforcer);
        ContribFixture.Solved s = f.solve(enforcer.penalty());
        assertTrue(s.solved());
        FamilyKit.Sink sink = FamilyKit.run(family, f.problem, f.view, s.toSolution(), c);
        assertEquals(2, sink.measure());                  // windows [d0..d2] and [d1..d3]
        assertEquals(sink.measure(), Math.round(s.objective()));
        assertEquals(2, enforcer.satisfied.size());       // one soft literal per window
    }

    @Test
    void softWithTwoResourcesCostsNothing() {
        Constraint c = constraint(IEnumEnforcement.Types.SOFT, 2);
        ContribFixture f = fixture(c, "ana", "ben");
        ContribFixture.FakeEnforcer enforcer = new ContribFixture.FakeEnforcer(Enforcer.Mode.SOFT, f);
        contributor.apply(f.ctx, f.compile(family, c), enforcer);
        ContribFixture.Solved s = f.solve(enforcer.penalty());
        assertEquals(0, Math.round(s.objective()));
    }

    @Test
    void relaxableFollowsTheSharedLiteral() {
        Constraint c = constraint(IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION, 2);
        ContribFixture on = fixture(c, "ana");
        ContribFixture.FakeEnforcer active = new ContribFixture.FakeEnforcer(Enforcer.Mode.RELAXABLE, on);
        contributor.apply(on.ctx, on.compile(family, c), active);
        on.model.addEquality(active.shared(), 1);
        assertTrue(on.solve(null).infeasible());

        ContribFixture off = fixture(c, "ana");
        ContribFixture.FakeEnforcer dropped = new ContribFixture.FakeEnforcer(Enforcer.Mode.RELAXABLE, off);
        contributor.apply(off.ctx, off.compile(family, c), dropped);
        off.model.addEquality(dropped.shared(), 0);
        assertTrue(off.solve(null).solved());
    }

    @Test
    void explainModeGuardsTheConstraintWithTheAssumptionLiteral() {
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED, 2);
        ContribFixture f = fixture(c, "ana");
        f.explain = true;
        ContribFixture.FakeEnforcer enforcer = new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, f);
        contributor.apply(f.ctx, f.compile(family, c), enforcer);
        f.model.addEquality(enforcer.shared(), 1);
        assertTrue(f.solve(null).infeasible());
    }

    @Test
    void periodLongerThanTheHorizonAddsNothing() {
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED, 10);
        ContribFixture f = fixture(c, "ana");
        contributor.apply(f.ctx, f.compile(family, c), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, f));
        assertTrue(f.solve(null).solved());
    }
}
