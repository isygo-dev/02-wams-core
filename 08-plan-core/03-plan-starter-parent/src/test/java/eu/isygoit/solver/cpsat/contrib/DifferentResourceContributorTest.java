package eu.isygoit.solver.cpsat.contrib;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.family.DifferentResourceFamily;
import eu.isygoit.solver.family.FamilyKit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static eu.isygoit.solver.family.FamilyKit.map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DifferentResourceContributorTest {

    private final DifferentResourceFamily family = new DifferentResourceFamily();
    private final DifferentResourceContributor contributor = new DifferentResourceContributor();

    @BeforeEach
    void requiresNatives() {
        Assumptions.assumeTrue(ContribFixture.nativeAvailable(), "OR-Tools natives not available");
    }

    private Constraint constraint(IEnumEnforcement.Types enforcement) {
        return FamilyKit.constraint("diff", "different_resource", enforcement, null,
                enforcement == IEnumEnforcement.Types.SOFT ? 1 : 0, map("groupByTagPrefix", "exam-"));
    }

    private ContribFixture fixture(Constraint c, int resources) {
        List<Resource> rs = new ArrayList<>();
        for (int i = 1; i <= resources; i++) {
            rs.add(FamilyKit.resource("r" + i, 1));
        }
        PlanningProblem p = FamilyKit.problem(FamilyKit.horizon(40, 3600), "UTC", rs,
                List.of(FamilyKit.task("a", 2, "exam-1"), FamilyKit.task("b", 2, "exam-1"), FamilyKit.task("c", 2, "exam-1")),
                List.of(c));
        return ContribFixture.of(FamilyKit.View.of(p).allCandidates());
    }

    @Test
    void identityAndSupport() {
        assertEquals("different_resource", contributor.type());
        assertEquals(EnforcementSupport.ALL, contributor.support());
    }

    @Test
    void hardPigeonholeIsInfeasibleAndEnoughResourcesAreAllDifferent() {
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED);
        ContribFixture two = fixture(c, 2);
        contributor.apply(two.ctx, two.compile(family, c), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, two));
        assertTrue(two.solve(null).infeasible());

        ContribFixture three = fixture(c, 3);
        contributor.apply(three.ctx, three.compile(family, c), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, three));
        ContribFixture.Solved s = three.solve(null);
        assertTrue(s.solved());
        assertEquals(0, FamilyKit.run(family, three.problem, three.view, s.toSolution(), c).count());
        assertFalse(s.on("a", "r1") && s.on("b", "r1"));
    }

    @Test
    void softPenaltyIsTheNumberOfExtraTasksAndMatchesTheChecker() {
        Constraint c = constraint(IEnumEnforcement.Types.SOFT);
        ContribFixture f = fixture(c, 2);
        ContribFixture.FakeEnforcer e = new ContribFixture.FakeEnforcer(Enforcer.Mode.SOFT, f);
        contributor.apply(f.ctx, f.compile(family, c), e);
        ContribFixture.Solved s = f.solve(e.penalty());
        assertEquals(1, Math.round(s.objective()));          // 3 tasks on 2 resources: one extra
        assertEquals(1, FamilyKit.run(family, f.problem, f.view, s.toSolution(), c).measure());

        ContribFixture roomy = fixture(c, 3);
        ContribFixture.FakeEnforcer e2 = new ContribFixture.FakeEnforcer(Enforcer.Mode.SOFT, roomy);
        contributor.apply(roomy.ctx, roomy.compile(family, c), e2);
        assertEquals(0, Math.round(roomy.solve(e2.penalty()).objective()));

        ContribFixture single = fixture(c, 1);
        ContribFixture.FakeEnforcer e3 = new ContribFixture.FakeEnforcer(Enforcer.Mode.SOFT, single);
        contributor.apply(single.ctx, single.compile(family, c), e3);
        ContribFixture.Solved forced = single.solve(e3.penalty());
        assertEquals(2, Math.round(forced.objective()));
        assertEquals(2, FamilyKit.run(family, single.problem, single.view, forced.toSolution(), c).measure());
    }

    @Test
    void relaxableAndExplainFollowTheSharedLiteral() {
        Constraint c = constraint(IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION);
        ContribFixture on = fixture(c, 2);
        ContribFixture.FakeEnforcer active = new ContribFixture.FakeEnforcer(Enforcer.Mode.RELAXABLE, on);
        contributor.apply(on.ctx, on.compile(family, c), active);
        on.model.addEquality(active.shared(), 1);
        assertTrue(on.solve(null).infeasible());

        ContribFixture off = fixture(c, 2);
        ContribFixture.FakeEnforcer dropped = new ContribFixture.FakeEnforcer(Enforcer.Mode.RELAXABLE, off);
        contributor.apply(off.ctx, off.compile(family, c), dropped);
        off.model.addEquality(dropped.shared(), 0);
        assertTrue(off.solve(null).solved());

        Constraint hard = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED);
        ContribFixture explain = fixture(hard, 2);
        explain.explain = true;
        ContribFixture.FakeEnforcer assumption = new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, explain);
        contributor.apply(explain.ctx, explain.compile(family, hard), assumption);
        explain.model.addEquality(assumption.shared(), 1);
        assertTrue(explain.solve(null).infeasible());
    }
}
