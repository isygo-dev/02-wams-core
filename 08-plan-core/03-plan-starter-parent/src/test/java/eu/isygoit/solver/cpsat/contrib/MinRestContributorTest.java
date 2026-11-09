package eu.isygoit.solver.cpsat.contrib;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.exception.LimitExceededException;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.family.FamilyKit;
import eu.isygoit.solver.family.MinRestFamily;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static eu.isygoit.solver.family.FamilyKit.map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinRestContributorTest {

    private final MinRestFamily family = new MinRestFamily();
    private final MinRestContributor contributor = new MinRestContributor();

    @BeforeEach
    void requiresNatives() {
        Assumptions.assumeTrue(ContribFixture.nativeAvailable(), "OR-Tools natives not available");
    }

    private Constraint rest(IEnumEnforcement.Types enforcement, int ticks) {
        return FamilyKit.constraint("rest", "min_rest", enforcement, null,
                enforcement == IEnumEnforcement.Types.SOFT ? 1 : 0, map("minRestTicks", ticks));
    }

    /** Two tasks A and B of {@code duration} ticks; windows {@code [estA..lstA]} and {@code [estB..lstB]}. */
    private ContribFixture fixture(Constraint c, int horizon, int duration, int[] winA, int[] winB, int capacity,
                                   String... resources) {
        List<Resource> rs = new ArrayList<>();
        for (String r : resources) {
            rs.add(FamilyKit.resource(r, capacity));
        }
        PlanningProblem p = FamilyKit.problem(FamilyKit.horizon(horizon, 3600), "UTC", rs,
                List.of(FamilyKit.task("A", duration), FamilyKit.task("B", duration)), List.of(c));
        FamilyKit.View v = FamilyKit.View.of(p).allCandidates();
        v.window("A", winA[0], winA[1]).window("B", winB[0], winB[1]);
        return ContribFixture.of(v);
    }

    @Test
    void identityAndSupport() {
        assertEquals("min_rest", contributor.type());
        assertEquals(EnforcementSupport.ALL, contributor.support());
    }

    @Test
    void staticPairsBecomeClausesAndNeedDifferentResources() {
        // roster case: A [8,16) and B [18,26) pinned, rest 11: the same nurse cannot take both
        Constraint c = rest(IEnumEnforcement.Types.STRICTLY_REQUIRED, 11);
        ContribFixture one = fixture(c, 48, 8, new int[]{8, 8}, new int[]{18, 18}, 1, "ana");
        contributor.apply(one.ctx, one.compile(family, c), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, one));
        assertTrue(one.solve(null).infeasible());

        ContribFixture two = fixture(c, 48, 8, new int[]{8, 8}, new int[]{18, 18}, 1, "ana", "ben");
        contributor.apply(two.ctx, two.compile(family, c), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, two));
        ContribFixture.Solved s = two.solve(null);
        assertTrue(s.solved());
        assertEquals(s.on("A", "ana"), s.on("B", "ben"));
        // the static shortcut creates no interval at all
        assertEquals(0, two.model.model().getConstraintsList().stream().filter(k -> k.hasNoOverlap()).count());
    }

    @Test
    void staticPairFarEnoughApartIsFree() {
        Constraint c = rest(IEnumEnforcement.Types.STRICTLY_REQUIRED, 11);
        ContribFixture f = fixture(c, 60, 8, new int[]{0, 0}, new int[]{19, 19}, 1, "ana");
        contributor.apply(f.ctx, f.compile(family, c), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, f));
        assertTrue(f.solve(null).solved());
    }

    @Test
    void movableTasksUseInflatedNoOverlap() {
        Constraint c = rest(IEnumEnforcement.Types.STRICTLY_REQUIRED, 3);
        ContribFixture f = fixture(c, 20, 4, new int[]{0, 16}, new int[]{0, 16}, 1, "ana");
        contributor.apply(f.ctx, f.compile(family, c), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, f));
        assertEquals(1, f.model.model().getConstraintsList().stream().filter(k -> k.hasNoOverlap()).count());
        ContribFixture.Solved s = f.solve(null);
        assertTrue(s.solved());
        long a = s.startOf("A");
        long b = s.startOf("B");
        assertTrue(Math.abs(a - b) >= 4 + 3, "gap must be at least the rest: " + a + " / " + b);
        FamilyKit.Sink sink = FamilyKit.run(family, f.problem, f.view, s.toSolution(), c);
        assertEquals(0, sink.count());
    }

    @Test
    void movableTasksInTooSmallAHorizonAreInfeasible() {
        Constraint c = rest(IEnumEnforcement.Types.STRICTLY_REQUIRED, 3);
        ContribFixture f = fixture(c, 10, 4, new int[]{0, 6}, new int[]{0, 6}, 1, "ana");
        contributor.apply(f.ctx, f.compile(family, c), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, f));
        assertTrue(f.solve(null).infeasible());
    }

    @Test
    void softStaticPairCostsOneAndMatchesTheChecker() {
        Constraint c = rest(IEnumEnforcement.Types.SOFT, 11);
        ContribFixture f = fixture(c, 48, 8, new int[]{8, 8}, new int[]{18, 18}, 1, "ana");
        ContribFixture.FakeEnforcer e = new ContribFixture.FakeEnforcer(Enforcer.Mode.SOFT, f);
        contributor.apply(f.ctx, f.compile(family, c), e);
        ContribFixture.Solved s = f.solve(e.penalty());
        assertEquals(1, Math.round(s.objective()));
        assertEquals(1, FamilyKit.run(family, f.problem, f.view, s.toSolution(), c).measure());
    }

    @Test
    void softMovablePairCanAvoidTheCostWhenThereIsRoomButNotWhenThereIsNot() {
        Constraint c = rest(IEnumEnforcement.Types.SOFT, 3);
        ContribFixture roomy = fixture(c, 20, 4, new int[]{0, 16}, new int[]{0, 16}, 1, "ana");
        ContribFixture.FakeEnforcer e1 = new ContribFixture.FakeEnforcer(Enforcer.Mode.SOFT, roomy);
        contributor.apply(roomy.ctx, roomy.compile(family, c), e1);
        assertEquals(0, Math.round(roomy.solve(e1.penalty()).objective()));

        ContribFixture tight = fixture(c, 9, 4, new int[]{0, 5}, new int[]{0, 5}, 1, "ana");
        ContribFixture.FakeEnforcer e2 = new ContribFixture.FakeEnforcer(Enforcer.Mode.SOFT, tight);
        contributor.apply(tight.ctx, tight.compile(family, c), e2);
        ContribFixture.Solved s = tight.solve(e2.penalty());
        assertEquals(1, Math.round(s.objective()));
        assertEquals(1, FamilyKit.run(family, tight.problem, tight.view, s.toSolution(), c).measure());
    }

    @Test
    void relaxableAndExplainFollowTheSharedLiteral() {
        Constraint c = rest(IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION, 11);
        ContribFixture on = fixture(c, 48, 8, new int[]{8, 8}, new int[]{18, 18}, 1, "ana");
        ContribFixture.FakeEnforcer active = new ContribFixture.FakeEnforcer(Enforcer.Mode.RELAXABLE, on);
        contributor.apply(on.ctx, on.compile(family, c), active);
        on.model.addEquality(active.shared(), 1);
        assertTrue(on.solve(null).infeasible());

        ContribFixture off = fixture(c, 48, 8, new int[]{8, 8}, new int[]{18, 18}, 1, "ana");
        ContribFixture.FakeEnforcer dropped = new ContribFixture.FakeEnforcer(Enforcer.Mode.RELAXABLE, off);
        contributor.apply(off.ctx, off.compile(family, c), dropped);
        off.model.addEquality(dropped.shared(), 0);
        assertTrue(off.solve(null).solved());

        Constraint hard = rest(IEnumEnforcement.Types.STRICTLY_REQUIRED, 11);
        ContribFixture explain = fixture(hard, 48, 8, new int[]{8, 8}, new int[]{18, 18}, 1, "ana");
        explain.explain = true;
        ContribFixture.FakeEnforcer assumption = new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, explain);
        contributor.apply(explain.ctx, explain.compile(family, hard), assumption);
        explain.model.addEquality(assumption.shared(), 1);
        assertTrue(explain.solve(null).infeasible());
    }

    @Test
    void afterTasksOnlyInflatesTheSelectedTasks() {
        Constraint c = FamilyKit.hard("rest", "min_rest", map("minRestTicks", 11,
                "afterTasks", map("kind", "task", "value", "B")));
        // A [0,8) then B [10,18): only B is followed by a rest, so A before B is fine on one resource
        ContribFixture f = fixture(c, 40, 8, new int[]{0, 0}, new int[]{10, 10}, 1, "ana");
        contributor.apply(f.ctx, f.compile(family, c), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, f));
        assertTrue(f.solve(null).solved());
    }

    @Test
    void nonUnaryResourcesAreSkipped() {
        Constraint c = rest(IEnumEnforcement.Types.STRICTLY_REQUIRED, 11);
        ContribFixture f = fixture(c, 48, 8, new int[]{8, 8}, new int[]{18, 18}, 2, "pool");
        contributor.apply(f.ctx, f.compile(family, c), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, f));
        assertTrue(f.solve(null).solved());
    }

    @Test
    void pairwiseGuardFailsTheSolvePassAndSkipsTheExplainPass() {
        Constraint c = rest(IEnumEnforcement.Types.SOFT, 3);
        ContribFixture f = fixture(c, 20, 4, new int[]{0, 16}, new int[]{0, 16}, 1, "ana");
        f.view.limits(0);
        assertThrows(LimitExceededException.class, () -> contributor.apply(f.ctx, f.compile(family, c),
                new ContribFixture.FakeEnforcer(Enforcer.Mode.SOFT, f)));
        ContribFixture g = fixture(c, 20, 4, new int[]{0, 16}, new int[]{0, 16}, 1, "ana");
        g.view.limits(0);
        g.explain = true;
        contributor.apply(g.ctx, g.compile(family, c), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, g));
        assertTrue(g.solve(null).solved());
    }
}
