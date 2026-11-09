package eu.isygoit.solver.cpsat.contrib;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.family.FamilyKit;
import eu.isygoit.solver.family.SameResourceFamily;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static eu.isygoit.solver.family.FamilyKit.map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SameResourceContributorTest {

    private final SameResourceFamily family = new SameResourceFamily();
    private final SameResourceContributor contributor = new SameResourceContributor();

    @BeforeEach
    void requiresNatives() {
        Assumptions.assumeTrue(ContribFixture.nativeAvailable(), "OR-Tools natives not available");
    }

    private Constraint constraint(IEnumEnforcement.Types enforcement) {
        return FamilyKit.constraint("same", "same_resource", enforcement, null, 0, map("groupByTagPrefix", "case-"));
    }

    /** Tasks a and b in case-1 (windows disjoint), r1 and r2; {@code aOn} / {@code bOn} list the candidate resources. */
    private ContribFixture fixture(Constraint c, String[] aOn, String[] bOn, String... optional) {
        PlanningProblem p = FamilyKit.problem(FamilyKit.horizon(40, 3600), "UTC",
                List.of(FamilyKit.resource("r1", 1), FamilyKit.resource("r2", 1)),
                List.of(FamilyKit.task("a", 2, "case-1"), FamilyKit.task("b", 2, "case-1")), List.of(c));
        FamilyKit.View v = FamilyKit.View.of(p);
        for (String r : aOn) {
            v.candidate("a", r);
        }
        for (String r : bOn) {
            v.candidate("b", r);
        }
        return ContribFixture.of(v, optional);
    }

    @Test
    void identityAndSupport() {
        assertEquals("same_resource", contributor.type());
        assertEquals(new EnforcementSupport(true, false, true), contributor.support());
    }

    @Test
    void hardForcesTheSameResource() {
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED);
        ContribFixture f = fixture(c, new String[]{"r1", "r2"}, new String[]{"r1", "r2"});
        contributor.apply(f.ctx, f.compile(family, c), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, f));
        f.model.addEquality(f.cand[f.view.candidateBegin(f.view.taskIdx("a")) + 1], 1);   // a on r2
        ContribFixture.Solved s = f.solve(null);
        assertTrue(s.solved());
        assertTrue(s.on("a", "r2") && s.on("b", "r2"));
        assertEquals(0, FamilyKit.run(family, f.problem, f.view, s.toSolution(), c).count());
    }

    @Test
    void disjointCandidateSetsAreInfeasibleUnlessRelaxed() {
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED);
        ContribFixture hard = fixture(c, new String[]{"r1"}, new String[]{"r2"});
        contributor.apply(hard.ctx, hard.compile(family, c), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, hard));
        assertTrue(hard.solve(null).infeasible());

        Constraint relaxable = constraint(IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION);
        ContribFixture on = fixture(relaxable, new String[]{"r1"}, new String[]{"r2"});
        ContribFixture.FakeEnforcer active = new ContribFixture.FakeEnforcer(Enforcer.Mode.RELAXABLE, on);
        contributor.apply(on.ctx, on.compile(family, relaxable), active);
        on.model.addEquality(active.shared(), 1);
        assertTrue(on.solve(null).infeasible());

        ContribFixture off = fixture(relaxable, new String[]{"r1"}, new String[]{"r2"});
        ContribFixture.FakeEnforcer dropped = new ContribFixture.FakeEnforcer(Enforcer.Mode.RELAXABLE, off);
        contributor.apply(off.ctx, off.compile(family, relaxable), dropped);
        off.model.addEquality(dropped.shared(), 0);
        ContribFixture.Solved s = off.solve(null);
        assertTrue(s.solved());
        assertEquals(1, FamilyKit.run(family, off.problem, off.view, s.toSolution(), relaxable).count());
    }

    @Test
    void anAbsentOptionalTaskDoesNotConstrainTheOther() {
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED);
        ContribFixture f = fixture(c, new String[]{"r1"}, new String[]{"r2"}, "b");
        contributor.apply(f.ctx, f.compile(family, c), new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, f));
        f.model.addEquality(f.presence[f.view.taskIdx("b")], 0);
        assertTrue(f.solve(null).solved());
    }

    @Test
    void explainModeGuardsTheClausesWithTheAssumptionLiteral() {
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED);
        ContribFixture f = fixture(c, new String[]{"r1"}, new String[]{"r2"});
        f.explain = true;
        ContribFixture.FakeEnforcer assumption = new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, f);
        contributor.apply(f.ctx, f.compile(family, c), assumption);
        f.model.addEquality(assumption.shared(), 1);
        assertTrue(f.solve(null).infeasible());
    }
}
