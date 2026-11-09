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
import java.util.Set;

import static eu.isygoit.solver.family.FamilyKit.map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinRestFamilyTest {

    private final MinRestFamily family = new MinRestFamily();

    private PlanningProblem problem(Constraint c, int capacity) {
        return FamilyKit.problem(FamilyKit.horizon(100, 3600), "UTC", List.of(FamilyKit.resource("r1", capacity)),
                List.of(FamilyKit.task("A", 8, "x"), FamilyKit.task("B", 8, "y"), FamilyKit.task("C", 8, "y")), List.of(c));
    }

    private Sink check(Constraint c, int[][] slots) {
        PlanningProblem p = problem(c, 1);
        View v = View.of(p).allCandidates();
        Solution s = new Solution(v);
        String[] ids = {"A", "B", "C"};
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null) {
                s.assign(ids[i], slots[i][0], slots[i][1], "r1");
            }
        }
        return FamilyKit.run(family, p, v, s, c);
    }

    private Constraint rest(int ticks) {
        return FamilyKit.hard("rest", "min_rest", map("minRestTicks", ticks));
    }

    @Test
    void identity() {
        assertEquals("min_rest", family.type());
        assertEquals(ScopeKind.RESOURCES, family.scopeKind());
        assertTrue(family.supportedEnforcements().containsAll(Set.of(
                IEnumEnforcement.Types.STRICTLY_REQUIRED, IEnumEnforcement.Types.SOFT,
                IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION)));
    }

    @Test
    void validateRejectsNonUnaryResourcesAndNegativeRest() {
        Constraint c = rest(11);
        Validation ok = new Validation(View.of(problem(c, 1)));
        family.validate(ok, c, FamilyKit.bind(family, c));
        assertTrue(ok.errors.isEmpty());

        Validation notUnary = new Validation(View.of(problem(c, 2)));
        family.validate(notUnary, c, FamilyKit.bind(family, c));
        assertTrue(notUnary.hasError("MIN_REST_RESOURCE_NOT_UNARY"));

        Constraint negative = rest(-1);
        Validation v = new Validation(View.of(problem(negative, 1)));
        family.validate(v, negative, FamilyKit.bind(family, negative));
        assertTrue(v.hasError("PARAMS_INVALID"));

        Constraint zero = rest(0);
        Validation w = new Validation(View.of(problem(zero, 1)));
        family.validate(w, zero, FamilyKit.bind(family, zero));
        assertTrue(w.errors.isEmpty());
        assertEquals(1, w.warnings.size());
    }

    @Test
    void validateChecksAfterTasksReference() {
        Constraint c = FamilyKit.hard("rest", "min_rest", map("minRestTicks", 5,
                "afterTasks", map("kind", "task", "value", "nope")));
        Validation v = new Validation(View.of(problem(c, 1)));
        family.validate(v, c, FamilyKit.bind(family, c));
        assertTrue(v.hasError("UNKNOWN_REFERENCE"));
    }

    @Test
    void estimateDependsOnEnforcement() {
        Constraint hard = rest(11);
        PlanningProblem p = problem(hard, 1);
        View v = View.of(p).allCandidates();
        ModelCost hardCost = family.estimate(v, hard, FamilyKit.bind(family, hard));
        assertEquals(3, hardCost.intervals());
        assertEquals(0, hardCost.pairwise());

        Constraint soft = FamilyKit.soft("rest", "min_rest", 5, map("minRestTicks", 11));
        ModelCost softCost = family.estimate(v, soft, FamilyKit.bind(family, soft));
        assertEquals(3, softCost.pairwise());   // all three tasks can overlap their rest window
    }

    @Test
    void checkerCountsClosePairs() {
        // A [0,8), B [10,18): gap 2
        assertEquals(1, check(rest(11), new int[][]{{0, 8}, {10, 18}, null}).count());
        assertEquals(0, check(rest(2), new int[][]{{0, 8}, {10, 18}, null}).count());
        assertEquals(1, check(rest(3), new int[][]{{0, 8}, {10, 18}, null}).count());
        // three consecutive close shifts: every pair is inside the rest window of the earlier one
        assertEquals(3, check(rest(30), new int[][]{{0, 8}, {10, 18}, {20, 28}}).count());
        assertEquals(0, check(rest(11), new int[][]{{0, 8}, {19, 27}, {38, 46}}).count());
    }

    @Test
    void afterTasksRestrictsWhichTasksAreInflated() {
        Constraint onlyB = FamilyKit.hard("rest", "min_rest", map("minRestTicks", 11,
                "afterTasks", map("kind", "task", "value", "B")));
        // A first, B second: only B has a rest after it, A ends at 8 and B starts at 10 -> fine
        assertEquals(0, check(onlyB, new int[][]{{0, 8}, {10, 18}, null}).count());
        // B first (ends 18, rest until 29), A second starts at 20 -> violation
        assertEquals(1, check(onlyB, new int[][]{{20, 28}, {10, 18}, null}).count());
        Constraint tag = FamilyKit.hard("rest", "min_rest", map("minRestTicks", 11,
                "afterTasks", map("kind", "tag", "value", "x")));
        assertEquals(1, check(tag, new int[][]{{0, 8}, {10, 18}, null}).count());
    }

    @Test
    void zeroLengthTaskWithoutRestNeverConflicts() {
        Constraint c = rest(0);
        PlanningProblem p = FamilyKit.problem(FamilyKit.horizon(50, 3600), "UTC", List.of(FamilyKit.resource("r1", 1)),
                List.of(FamilyKit.task("A", 0), FamilyKit.task("B", 8)), List.of(c));
        View v = View.of(p).allCandidates();
        Solution s = new Solution(v).assign("A", 4, 4, "r1").assign("B", 0, 8, "r1");
        assertEquals(0, FamilyKit.run(family, p, v, s, c).count());
    }
}
