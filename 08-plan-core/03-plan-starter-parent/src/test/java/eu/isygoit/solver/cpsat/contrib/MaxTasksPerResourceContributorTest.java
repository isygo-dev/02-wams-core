package eu.isygoit.solver.cpsat.contrib;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.family.FamilyKit;
import eu.isygoit.solver.family.MaxTasksPerResourceFamily;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static eu.isygoit.solver.family.FamilyKit.map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaxTasksPerResourceContributorTest {

    private final MaxTasksPerResourceFamily family = new MaxTasksPerResourceFamily();
    private final MaxTasksPerResourceContributor contributor = new MaxTasksPerResourceContributor();

    @BeforeEach
    void requiresNatives() {
        Assumptions.assumeTrue(ContribFixture.nativeAvailable(), "OR-Tools natives not available");
    }

    private Constraint constraint(IEnumEnforcement.Types enforcement, Map<String, Object> params) {
        return FamilyKit.constraint("cap", "max_tasks_per_resource", enforcement, null,
                enforcement == IEnumEnforcement.Types.SOFT ? 1 : 0, params);
    }

    private ContribFixture fixture(Constraint c, int tasks, String... resources) {
        List<Resource> rs = new ArrayList<>();
        for (String r : resources) {
            rs.add(FamilyKit.resource(r, 2));
        }
        List<eu.isygoit.dto.request.Task> ts = new ArrayList<>();
        for (int i = 0; i < tasks; i++) {
            ts.add(FamilyKit.task("t" + i, 2));
        }
        PlanningProblem p = FamilyKit.problem(FamilyKit.horizon(48, 3600), "UTC", rs, ts, List.of(c));
        return ContribFixture.of(FamilyKit.View.of(p).allCandidates());
    }

    @Test
    void identityAndSupport() {
        assertEquals("max_tasks_per_resource", contributor.type());
        assertEquals(EnforcementSupport.ALL, contributor.support());
    }

    @Test
    void hardMaxOverTheHorizon() {
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED, map("max", 2));
        ContribFixture one = fixture(c, 3, "ana");
        contributor.apply(one.ctx, one.compile(family, c), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, one));
        assertTrue(one.solve(null).infeasible());

        ContribFixture two = fixture(c, 3, "ana", "ben");
        contributor.apply(two.ctx, two.compile(family, c), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, two));
        ContribFixture.Solved s = two.solve(null);
        assertTrue(s.solved());
        assertEquals(0, FamilyKit.run(family, two.problem, two.view, s.toSolution(), c).count());
    }

    @Test
    void softExcessMatchesTheChecker() {
        Constraint c = constraint(IEnumEnforcement.Types.SOFT, map("max", 2));
        ContribFixture f = fixture(c, 3, "ana");
        ContribFixture.FakeEnforcer e = new ContribFixture.FakeEnforcer(Enforcer.Mode.SOFT, f);
        contributor.apply(f.ctx, f.compile(family, c), e);
        ContribFixture.Solved s = f.solve(e.penalty());
        assertEquals(1, Math.round(s.objective()));
        assertEquals(1, FamilyKit.run(family, f.problem, f.view, s.toSolution(), c).measure());
    }

    @Test
    void minSpreadsTheTasksOverResources() {
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED, map("min", 1));
        ContribFixture f = fixture(c, 2, "ana", "ben");
        contributor.apply(f.ctx, f.compile(family, c), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, f));
        ContribFixture.Solved s = f.solve(null);
        assertTrue(s.solved());
        assertTrue(s.on("t0", "ana") != s.on("t1", "ana"));

        ContribFixture short1 = fixture(c, 1, "ana", "ben");
        contributor.apply(short1.ctx, short1.compile(family, c), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, short1));
        assertTrue(short1.solve(null).infeasible());
    }

    @Test
    void softMinDeficit() {
        Constraint c = constraint(IEnumEnforcement.Types.SOFT, map("min", 2));
        ContribFixture f = fixture(c, 2, "ana", "ben");
        ContribFixture.FakeEnforcer e = new ContribFixture.FakeEnforcer(Enforcer.Mode.SOFT, f);
        contributor.apply(f.ctx, f.compile(family, c), e);
        ContribFixture.Solved s = f.solve(e.penalty());
        assertEquals(2, Math.round(s.objective()));          // 2 tasks cannot give both resources 2 tasks
        assertEquals(2, FamilyKit.run(family, f.problem, f.view, s.toSolution(), c).measure());
    }

    @Test
    void perPeriodUsesTheStartPeriod() {
        Constraint c = constraint(IEnumEnforcement.Types.SOFT, map("max", 1, "period", map("unit", "DAY")));
        ContribFixture f = fixture(c, 2, "ana");
        ContribFixture.FakeEnforcer e = new ContribFixture.FakeEnforcer(Enforcer.Mode.SOFT, f);
        contributor.apply(f.ctx, f.compile(family, c), e);
        ContribFixture.Solved s = f.solve(e.penalty());
        assertEquals(0, Math.round(s.objective()));           // the two tasks start on different days
        assertTrue(Math.floorDiv(s.startOf("t0"), 24) != Math.floorDiv(s.startOf("t1"), 24));
    }

    @Test
    void relaxableAndExplainFollowTheSharedLiteral() {
        Constraint c = constraint(IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION, map("max", 1));
        ContribFixture on = fixture(c, 2, "ana");
        ContribFixture.FakeEnforcer active = new ContribFixture.FakeEnforcer(Enforcer.Mode.RELAXABLE, on);
        contributor.apply(on.ctx, on.compile(family, c), active);
        on.model.addEquality(active.shared(), 1);
        assertTrue(on.solve(null).infeasible());

        ContribFixture off = fixture(c, 2, "ana");
        ContribFixture.FakeEnforcer dropped = new ContribFixture.FakeEnforcer(Enforcer.Mode.RELAXABLE, off);
        contributor.apply(off.ctx, off.compile(family, c), dropped);
        off.model.addEquality(dropped.shared(), 0);
        assertTrue(off.solve(null).solved());

        Constraint hard = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED, map("max", 1));
        ContribFixture explain = fixture(hard, 2, "ana");
        explain.explain = true;
        ContribFixture.FakeEnforcer assumption = new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, explain);
        contributor.apply(explain.ctx, explain.compile(family, hard), assumption);
        explain.model.addEquality(assumption.shared(), 1);
        assertTrue(explain.solve(null).infeasible());
    }
}
