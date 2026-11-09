package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class DifferentResourceFamilyTest {

    private final DifferentResourceFamily family = new DifferentResourceFamily();

    private Constraint constraint() {
        return FamilyKit.soft("diff", "different_resource", 3, map("groupByTagPrefix", "exam-"));
    }

    private PlanningProblem problem(Constraint c) {
        return FamilyKit.problem(FamilyKit.horizon(40, 3600), "UTC",
                List.of(FamilyKit.resource("r1", 1), FamilyKit.resource("r2", 1), FamilyKit.resource("r3", 1)),
                List.of(FamilyKit.task("a", 2, "exam-1"), FamilyKit.task("b", 2, "exam-1"), FamilyKit.task("c", 2, "exam-1")),
                List.of(c));
    }

    @Test
    void identity() {
        assertEquals("different_resource", family.type());
        assertEquals(ScopeKind.GROUPS, family.scopeKind());
        assertTrue(family.supportedEnforcements().contains(IEnumEnforcement.Types.SOFT));
    }

    @Test
    void validateNeedsAGroupDefinition() {
        Constraint none = FamilyKit.hard("diff", "different_resource", map());
        Validation v = new Validation(View.of(problem(none)));
        family.validate(v, none, FamilyKit.bind(family, none));
        assertTrue(v.hasError("PARAMS_INVALID"));
        Constraint ok = constraint();
        Validation w = new Validation(View.of(problem(ok)));
        family.validate(w, ok, FamilyKit.bind(family, ok));
        assertTrue(w.errors.isEmpty());
    }

    @Test
    void estimateHasAnExcessVariablePerCandidate() {
        Constraint c = constraint();
        PlanningProblem p = problem(c);
        ModelCost cost = family.estimate(View.of(p).allCandidates(), c, FamilyKit.bind(family, c));
        assertEquals(9, cost.intVars());
    }

    @Test
    void checkerCountsExtraTasksSharingAResource() {
        Constraint c = constraint();
        PlanningProblem p = problem(c);
        View v = View.of(p).allCandidates();
        Solution allSame = new Solution(v).assign("a", 0, 2, "r1").assign("b", 4, 6, "r1").assign("c", 8, 10, "r1");
        Sink sink = FamilyKit.run(family, p, v, allSame, c);
        assertEquals(1, sink.count());
        assertEquals(2, sink.measure());       // two extra tasks on r1

        Solution oneShared = new Solution(v).assign("a", 0, 2, "r1").assign("b", 4, 6, "r1").assign("c", 8, 10, "r2");
        assertEquals(1, FamilyKit.run(family, p, v, oneShared, c).measure());

        Solution distinct = new Solution(v).assign("a", 0, 2, "r1").assign("b", 4, 6, "r2").assign("c", 8, 10, "r3");
        assertEquals(0, FamilyKit.run(family, p, v, distinct, c).count());
    }

    @Test
    void roleFilterLooksAtThatRoleOnly() {
        Constraint c = FamilyKit.hard("diff", "different_resource", map("groupByTagPrefix", "exam-", "role", "room"));
        PlanningProblem p = problem(c);
        View v = View.of(p).allCandidates();
        Solution s = new Solution(v)
                .assign("a", 0, 2, "r1").addResource("a", "room", "r2")
                .assign("b", 4, 6, "r1").addResource("b", "room", "r3");
        assertEquals(0, FamilyKit.run(family, p, v, s, c).count());   // same "default" resource r1 is not looked at
        Solution clash = new Solution(v)
                .assign("a", 0, 2, "r1").addResource("a", "room", "r2")
                .assign("b", 4, 6, "r3").addResource("b", "room", "r2");
        assertEquals(1, FamilyKit.run(family, p, v, clash, c).measure());
    }
}
