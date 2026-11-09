package eu.isygoit.solver.cpsat.contrib;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.family.FamilyKit;
import eu.isygoit.solver.family.FixedAssignmentFamily;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static eu.isygoit.solver.family.FamilyKit.map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedAssignmentContributorTest {

    private static final String START_5H = FamilyKit.START.plusSeconds(5 * 3600L).toString();

    private final FixedAssignmentFamily family = new FixedAssignmentFamily();
    private final FixedAssignmentContributor contributor = new FixedAssignmentContributor();

    @BeforeEach
    void requiresNatives() {
        Assumptions.assumeTrue(ContribFixture.nativeAvailable(), "OR-Tools natives not available");
    }

    private Constraint constraint(IEnumEnforcement.Types enforcement, Map<String, Object> params) {
        return FamilyKit.constraint("fix", "fixed_assignment", enforcement, new ConstraintScope("task", "t1"),
                enforcement == IEnumEnforcement.Types.SOFT ? 1 : 0, params);
    }

    private ContribFixture fixture(Constraint c) {
        PlanningProblem p = FamilyKit.problem(FamilyKit.horizon(40, 3600), "UTC",
                List.of(FamilyKit.resource("m1", 1), FamilyKit.resource("m2", 1)),
                List.of(FamilyKit.task("t1", 2), FamilyKit.task("t2", 2)), List.of(c));
        return ContribFixture.of(FamilyKit.View.of(p).allCandidates());
    }

    private Map<String, Object> startAndResource() {
        return map("start", START_5H, "resourceIds", List.of("m1"));
    }

    @Test
    void identityAndSupport() {
        assertEquals("fixed_assignment", contributor.type());
        assertEquals(EnforcementSupport.ALL, contributor.support());
    }

    @Test
    void hardSolvePassIsANoOpBecausePreprocessAlreadyPruned() {
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED, startAndResource());
        ContribFixture f = fixture(c);
        int before = f.model.model().getConstraintsCount();
        contributor.apply(f.ctx, f.compile(family, c), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, f));
        assertEquals(before, f.model.model().getConstraintsCount());
    }

    @Test
    void softCostsNothingWhenTheValuesCanBeTaken() {
        Constraint c = constraint(IEnumEnforcement.Types.SOFT, startAndResource());
        ContribFixture f = fixture(c);
        ContribFixture.FakeEnforcer e = new ContribFixture.FakeEnforcer(Enforcer.Mode.SOFT, f);
        contributor.apply(f.ctx, f.compile(family, c), e);
        ContribFixture.Solved s = f.solve(e.penalty());
        assertEquals(0, Math.round(s.objective()));
        assertEquals(5, s.startOf("t1"));
        assertTrue(s.on("t1", "m1"));
        assertEquals(1, e.satisfied.size());   // ONE instance literal per task
    }

    @Test
    void softDeviationIsOneUnitPerTaskAndMatchesTheChecker() {
        Constraint c = constraint(IEnumEnforcement.Types.SOFT, startAndResource());
        ContribFixture f = fixture(c);
        ContribFixture.FakeEnforcer e = new ContribFixture.FakeEnforcer(Enforcer.Mode.SOFT, f);
        contributor.apply(f.ctx, f.compile(family, c), e);
        // something else forces the start and the resource away from the fixed values
        f.model.addGreaterOrEqual(f.start[0], 10);
        f.model.addEquality(f.cand[f.view.candidateBegin(0) + 1], 1);        // t1 on m2
        ContribFixture.Solved s = f.solve(e.penalty());
        assertEquals(1, Math.round(s.objective()));
        assertEquals(1, FamilyKit.run(family, f.problem, f.view, s.toSolution(), c).measure());
    }

    @Test
    void resourceOnlyRestriction() {
        Constraint c = constraint(IEnumEnforcement.Types.SOFT, map("resourceIds", List.of("m1")));
        ContribFixture f = fixture(c);
        ContribFixture.FakeEnforcer e = new ContribFixture.FakeEnforcer(Enforcer.Mode.SOFT, f);
        contributor.apply(f.ctx, f.compile(family, c), e);
        f.model.addEquality(f.cand[f.view.candidateBegin(0) + 1], 1);        // t1 forced on m2
        assertEquals(1, Math.round(f.solve(e.penalty()).objective()));
    }

    @Test
    void relaxableAndExplainFollowTheSharedLiteral() {
        Constraint relaxable = constraint(IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION, startAndResource());
        ContribFixture on = fixture(relaxable);
        ContribFixture.FakeEnforcer active = new ContribFixture.FakeEnforcer(Enforcer.Mode.RELAXABLE, on);
        contributor.apply(on.ctx, on.compile(family, relaxable), active);
        on.model.addGreaterOrEqual(on.start[0], 10);
        on.model.addEquality(active.shared(), 1);
        assertTrue(on.solve(null).infeasible());

        ContribFixture off = fixture(relaxable);
        ContribFixture.FakeEnforcer dropped = new ContribFixture.FakeEnforcer(Enforcer.Mode.RELAXABLE, off);
        contributor.apply(off.ctx, off.compile(family, relaxable), dropped);
        off.model.addGreaterOrEqual(off.start[0], 10);
        off.model.addEquality(dropped.shared(), 0);
        assertTrue(off.solve(null).solved());

        Constraint hard = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED, startAndResource());
        ContribFixture explain = fixture(hard);
        explain.explain = true;
        ContribFixture.FakeEnforcer assumption = new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, explain);
        contributor.apply(explain.ctx, explain.compile(family, hard), assumption);
        explain.model.addGreaterOrEqual(explain.start[0], 10);
        explain.model.addEquality(assumption.shared(), 1);
        assertTrue(explain.solve(null).infeasible());
    }
}
