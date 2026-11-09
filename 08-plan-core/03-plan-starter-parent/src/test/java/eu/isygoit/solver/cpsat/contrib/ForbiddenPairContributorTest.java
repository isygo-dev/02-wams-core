package eu.isygoit.solver.cpsat.contrib;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.ResourceRequirement;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.family.FamilyKit;
import eu.isygoit.solver.family.ForbiddenPairFamily;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static eu.isygoit.solver.family.FamilyKit.map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForbiddenPairContributorTest {

    private final ForbiddenPairFamily family = new ForbiddenPairFamily();
    private final ForbiddenPairContributor contributor = new ForbiddenPairContributor();

    @BeforeEach
    void requiresNatives() {
        Assumptions.assumeTrue(ContribFixture.nativeAvailable(), "OR-Tools natives not available");
    }

    private Constraint constraint(IEnumEnforcement.Types enforcement, Map<String, Object> params) {
        return FamilyKit.constraint("fp", "forbidden_pair", enforcement, null,
                enforcement == IEnumEnforcement.Types.SOFT ? 1 : 0, params);
    }

    private Map<String, Object> taskPair(String relation) {
        return map("kind", "TASK_TASK", "relation", relation, "pairs", List.of(map("a", "a", "b", "b")));
    }

    /** Tasks a and b, resources r1 r2; {@code pinned} fixes both at tick 0 (forced time overlap). */
    private ContribFixture fixture(Constraint c, int resources, boolean pinned) {
        List<eu.isygoit.dto.request.Resource> rs = new java.util.ArrayList<>();
        for (int i = 1; i <= resources; i++) {
            rs.add(FamilyKit.resource("r" + i, 4));
        }
        PlanningProblem p = FamilyKit.problem(FamilyKit.horizon(20, 3600), "UTC", rs,
                List.of(FamilyKit.task("a", 4), FamilyKit.task("b", 4)), List.of(c));
        FamilyKit.View v = FamilyKit.View.of(p).allCandidates();
        if (pinned) {
            v.pinned("a", 0).pinned("b", 0);
        }
        return ContribFixture.of(v);
    }

    @Test
    void identityAndSupport() {
        assertEquals("forbidden_pair", contributor.type());
        assertEquals(EnforcementSupport.ALL, contributor.support());
    }

    @Test
    void sameResourceHard() {
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED, taskPair("SAME_RESOURCE"));
        ContribFixture one = fixture(c, 1, false);
        contributor.apply(one.ctx, one.compile(family, c), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, one));
        assertTrue(one.solve(null).infeasible());

        ContribFixture two = fixture(c, 2, false);
        contributor.apply(two.ctx, two.compile(family, c), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, two));
        ContribFixture.Solved s = two.solve(null);
        assertTrue(s.solved());
        assertFalse(s.on("a", "r1") && s.on("b", "r1"));
        assertEquals(0, FamilyKit.run(family, two.problem, two.view, s.toSolution(), c).count());
    }

    @Test
    void sameResourceSoftIsOnePerPair() {
        Constraint c = constraint(IEnumEnforcement.Types.SOFT, taskPair("SAME_RESOURCE"));
        ContribFixture f = fixture(c, 1, false);
        ContribFixture.FakeEnforcer e = new ContribFixture.FakeEnforcer(Enforcer.Mode.SOFT, f);
        contributor.apply(f.ctx, f.compile(family, c), e);
        ContribFixture.Solved s = f.solve(e.penalty());
        assertEquals(1, Math.round(s.objective()));
        assertEquals(1, FamilyKit.run(family, f.problem, f.view, s.toSolution(), c).measure());
        assertEquals(1, e.satisfied.size());
    }

    @Test
    void timeOverlapHardSoftAndRelaxable() {
        Constraint hard = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED, taskPair("TIME_OVERLAP"));
        ContribFixture h = fixture(hard, 2, true);
        contributor.apply(h.ctx, h.compile(family, hard), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, h));
        assertTrue(h.solve(null).infeasible());

        Constraint soft = constraint(IEnumEnforcement.Types.SOFT, taskPair("TIME_OVERLAP"));
        ContribFixture f = fixture(soft, 2, true);
        ContribFixture.FakeEnforcer e = new ContribFixture.FakeEnforcer(Enforcer.Mode.SOFT, f);
        contributor.apply(f.ctx, f.compile(family, soft), e);
        ContribFixture.Solved s = f.solve(e.penalty());
        assertEquals(1, Math.round(s.objective()));
        assertEquals(1, FamilyKit.run(family, f.problem, f.view, s.toSolution(), soft).measure());

        ContribFixture free = fixture(hard, 2, false);
        contributor.apply(free.ctx, free.compile(family, hard), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, free));
        ContribFixture.Solved apart = free.solve(null);
        assertTrue(apart.solved());
        assertTrue(Math.abs(apart.startOf("a") - apart.startOf("b")) >= 4);

        Constraint relaxable = constraint(IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION, taskPair("TIME_OVERLAP"));
        ContribFixture off = fixture(relaxable, 2, true);
        ContribFixture.FakeEnforcer dropped = new ContribFixture.FakeEnforcer(Enforcer.Mode.RELAXABLE, off);
        contributor.apply(off.ctx, off.compile(family, relaxable), dropped);
        off.model.addEquality(dropped.shared(), 0);
        assertTrue(off.solve(null).solved());
    }

    @Test
    void resourcePairForbidsBothOnOneTask() {
        Map<String, Object> params = map("kind", "RESOURCE_RESOURCE", "pairs", List.of(map("a", "r1", "b", "r2")));
        Constraint hard = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED, params);
        PlanningProblem p = FamilyKit.problem(FamilyKit.horizon(20, 3600), "UTC",
                List.of(FamilyKit.resource("r1", 1), FamilyKit.resource("r2", 1)),
                List.of(FamilyKit.task("a", 4, List.of(
                        new ResourceRequirement("x", null, 1, null), new ResourceRequirement("y", null, 1, null)))),
                List.of(hard));
        FamilyKit.View v = FamilyKit.View.of(p).candidate("a", "r1", "x").candidate("a", "r2", "y");
        ContribFixture f = ContribFixture.of(v);
        contributor.apply(f.ctx, f.compile(family, hard), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, f));
        assertTrue(f.solve(null).infeasible());     // role x only on r1, role y only on r2: both are always together

        Constraint soft = constraint(IEnumEnforcement.Types.SOFT, params);
        ContribFixture g = ContribFixture.of(FamilyKit.View.of(p).candidate("a", "r1", "x").candidate("a", "r2", "y"));
        ContribFixture.FakeEnforcer e = new ContribFixture.FakeEnforcer(Enforcer.Mode.SOFT, g);
        contributor.apply(g.ctx, g.compile(family, soft), e);
        ContribFixture.Solved s = g.solve(e.penalty());
        assertEquals(1, Math.round(s.objective()));
        assertEquals(1, FamilyKit.run(family, g.problem, g.view, s.toSolution(), soft).measure());
    }

    @Test
    void explainModeGuardsTheClauses() {
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED, taskPair("SAME_RESOURCE"));
        ContribFixture f = fixture(c, 1, false);
        f.explain = true;
        ContribFixture.FakeEnforcer assumption = new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, f);
        contributor.apply(f.ctx, f.compile(family, c), assumption);
        f.model.addEquality(assumption.shared(), 1);
        assertTrue(f.solve(null).infeasible());
    }
}
