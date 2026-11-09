package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
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

class SameResourceFamilyTest {

    private final SameResourceFamily family = new SameResourceFamily();

    private Constraint byPrefix() {
        return FamilyKit.hard("same", "same_resource", map("groupByTagPrefix", "case-"));
    }

    private PlanningProblem problem(Constraint c) {
        return FamilyKit.problem(FamilyKit.horizon(40, 3600), "UTC",
                List.of(FamilyKit.resource("r1", 1), FamilyKit.resource("r2", 1)),
                List.of(FamilyKit.task("a", 2, "case-1"), FamilyKit.task("b", 2, "case-1"),
                        FamilyKit.task("c", 2, "case-2"), FamilyKit.task("d", 2, "case-2")), List.of(c));
    }

    @Test
    void identity() {
        assertEquals("same_resource", family.type());
        assertEquals(ScopeKind.GROUPS, family.scopeKind());
        assertTrue(family.supportedEnforcements().contains(IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION));
        assertFalse(family.supportedEnforcements().contains(IEnumEnforcement.Types.SOFT));
    }

    @Test
    void validateNeedsAPrefixOrAGroupScope() {
        Constraint none = FamilyKit.hard("same", "same_resource", map());
        Validation v = new Validation(View.of(problem(none)));
        family.validate(v, none, FamilyKit.bind(family, none));
        assertTrue(v.hasError("PARAMS_INVALID"));

        Constraint tag = FamilyKit.constraint("same", "same_resource", IEnumEnforcement.Types.STRICTLY_REQUIRED,
                new ConstraintScope("tag", "case-1"), 0, map());
        Validation w = new Validation(View.of(problem(tag)));
        family.validate(w, tag, FamilyKit.bind(family, tag));
        assertTrue(w.errors.isEmpty());

        Constraint ok = byPrefix();
        Validation x = new Validation(View.of(problem(ok)));
        family.validate(x, ok, FamilyKit.bind(family, ok));
        assertTrue(x.errors.isEmpty());
    }

    @Test
    void estimateCountsTwoClausesPerCandidate() {
        Constraint c = byPrefix();
        PlanningProblem p = problem(c);
        ModelCost cost = family.estimate(View.of(p).allCandidates(), c, FamilyKit.bind(family, c));
        assertEquals(2L * 4 * 2, cost.constraints());
    }

    @Test
    void checkerReportsPairsWithDifferentResources() {
        Constraint c = byPrefix();
        PlanningProblem p = problem(c);
        View v = View.of(p).allCandidates();
        Solution ok = new Solution(v).assign("a", 0, 2, "r1").assign("b", 4, 6, "r1")
                .assign("c", 0, 2, "r2").assign("d", 4, 6, "r2");
        assertEquals(0, FamilyKit.run(family, p, v, ok, c).count());

        Solution bad = new Solution(v).assign("a", 0, 2, "r1").assign("b", 4, 6, "r2")
                .assign("c", 0, 2, "r2").assign("d", 4, 6, "r1");
        Sink sink = FamilyKit.run(family, p, v, bad, c);
        assertEquals(2, sink.count());
        assertEquals(2, sink.measure());
    }

    @Test
    void unselectedTasksAreIgnored() {
        Constraint c = byPrefix();
        PlanningProblem p = problem(c);
        View v = View.of(p).allCandidates();
        Solution s = new Solution(v).assign("a", 0, 2, "r1").assign("c", 0, 2, "r2");
        assertEquals(0, FamilyKit.run(family, p, v, s, c).count());
    }

    @Test
    void roleFilterComparesOnlyThatRole() {
        Constraint c = FamilyKit.hard("same", "same_resource", map("groupByTagPrefix", "case-", "role", "teacher"));
        PlanningProblem p = problem(c);
        View v = View.of(p).allCandidates();
        Solution s = new Solution(v)
                .assign("a", 0, 2, "r1").addResource("a", "teacher", "r1")
                .assign("b", 4, 6, "r2").addResource("b", "teacher", "r1");
        Sink sink = FamilyKit.run(family, p, v, s, c);
        assertEquals(0, sink.count());   // "teacher" is r1 for both, the "default" roles differ but are ignored
    }

    @Test
    void groupScopeRestrictsTheMembers() {
        Constraint c = FamilyKit.constraint("same", "same_resource", IEnumEnforcement.Types.STRICTLY_REQUIRED,
                new ConstraintScope("tag", "case-2"), 0, map());
        PlanningProblem p = problem(c);
        View v = View.of(p).allCandidates();
        Solution s = new Solution(v).assign("a", 0, 2, "r1").assign("b", 4, 6, "r2")
                .assign("c", 0, 2, "r1").assign("d", 4, 6, "r1");
        assertEquals(0, FamilyKit.run(family, p, v, s, c).count());
    }
}
