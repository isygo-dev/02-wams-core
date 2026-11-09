package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.family.FamilyKit.Sink;
import eu.isygoit.solver.family.FamilyKit.Solution;
import eu.isygoit.solver.family.FamilyKit.Validation;
import eu.isygoit.solver.family.FamilyKit.View;
import eu.isygoit.solver.index.CandidateIndexBuilder;
import eu.isygoit.solver.spi.ModelCost;
import eu.isygoit.solver.spi.PreprocessContext;
import eu.isygoit.solver.spi.ScopeKind;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static eu.isygoit.solver.family.FamilyKit.map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedAssignmentFamilyTest {

    private final FixedAssignmentFamily family = new FixedAssignmentFamily();
    private static final String START_5H = FamilyKit.START.plusSeconds(5 * 3600L).toString();

    private Constraint constraint(IEnumEnforcement.Types enforcement, java.util.Map<String, Object> params) {
        return FamilyKit.constraint("fix", "fixed_assignment", enforcement, new ConstraintScope("task", "t1"), 2, params);
    }

    private PlanningProblem problem(Constraint c) {
        return FamilyKit.problem(FamilyKit.horizon(40, 3600), "UTC",
                List.of(FamilyKit.resource("m1", 1), FamilyKit.resource("m2", 1)),
                List.of(FamilyKit.task("t1", 2), FamilyKit.task("t2", 2)), List.of(c));
    }

    @Test
    void identity() {
        assertEquals("fixed_assignment", family.type());
        assertEquals(ScopeKind.TASKS, family.scopeKind());
        assertTrue(family.supportedEnforcements().contains(IEnumEnforcement.Types.SOFT));
        assertEquals(List.of("start", "resourceIds", "role"), family.paramsHint());
    }

    @Test
    void validate() {
        Constraint empty = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED, map());
        Validation v = new Validation(View.of(problem(empty)));
        family.validate(v, empty, FamilyKit.bind(family, empty));
        assertTrue(v.hasError("PARAMS_INVALID"));

        Constraint bad = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED,
                map("start", FamilyKit.START.plusSeconds(90).toString(), "resourceIds", List.of("ghost")));
        Validation w = new Validation(View.of(problem(bad)));
        family.validate(w, bad, FamilyKit.bind(family, bad));
        assertTrue(w.hasError("TIME_NOT_ALIGNED"));
        assertTrue(w.hasError("UNKNOWN_REFERENCE"));

        Constraint ok = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED,
                map("start", START_5H, "resourceIds", List.of("m1")));
        Validation x = new Validation(View.of(problem(ok)));
        family.validate(x, ok, FamilyKit.bind(family, ok));
        assertTrue(x.errors.isEmpty());
    }

    @Test
    void estimateCountsScopedTasksAndTheirCandidates() {
        Constraint c = constraint(IEnumEnforcement.Types.SOFT, map("resourceIds", List.of("m1")));
        View v = View.of(problem(c)).allCandidates();
        ModelCost cost = family.estimate(v, c, FamilyKit.bind(family, c));
        assertEquals(1, cost.boolVars());
        assertEquals(1 + 2, cost.constraints());
    }

    @Test
    void checkerCountsOneViolationPerDeviatingTask() {
        Constraint c = constraint(IEnumEnforcement.Types.SOFT, map("start", START_5H, "resourceIds", List.of("m1")));
        PlanningProblem p = problem(c);
        View v = View.of(p).allCandidates();

        assertEquals(0, FamilyKit.run(family, p, v, new Solution(v).assign("t1", 5, 7, "m1").assign("t2", 0, 2, "m2"), c).count());
        Sink wrongStart = FamilyKit.run(family, p, v, new Solution(v).assign("t1", 6, 8, "m1"), c);
        assertEquals(1, wrongStart.count());
        assertEquals(1, wrongStart.measure());
        Sink wrongResource = FamilyKit.run(family, p, v, new Solution(v).assign("t1", 5, 7, "m2"), c);
        assertEquals(1, wrongResource.count());
        assertEquals("m2", wrongResource.violations.get(0).resourceId());
        // both deviations of the same task are still one unit
        assertEquals(1, FamilyKit.run(family, p, v, new Solution(v).assign("t1", 9, 11, "m2"), c).count());
        // an unselected task is not checked
        assertEquals(0, FamilyKit.run(family, p, v, new Solution(v), c).count());
    }

    @Test
    void roleRestrictsTheResourceCheck() {
        Constraint c = constraint(IEnumEnforcement.Types.SOFT, map("resourceIds", List.of("m1"), "role", "operator"));
        PlanningProblem p = problem(c);
        View v = View.of(p).allCandidates();
        Solution s = new Solution(v).assign("t1", 0, 2, "m2").addResource("t1", "operator", "m1");
        assertEquals(0, FamilyKit.run(family, p, v, s, c).count());
    }

    @Test
    void preprocessRestrictsStartAndPrunesForeignCandidates() {
        Constraint c = constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED,
                map("start", START_5H, "resourceIds", List.of("m1")));
        PlanningProblem p = problem(c);
        View v = View.of(p).allCandidates();   // candidates: t1:m1(0) t1:m2(1) t2:m1(2) t2:m2(3)

        CandidateIndexBuilder.Mutable mutable = Mockito.mock(CandidateIndexBuilder.Mutable.class, inv -> {
            switch (inv.getMethod().getName()) {
                case "resourceIdx":
                    return v.resourceIdx((String) inv.getArgument(0));
                case "candidateBegin":
                    return v.candidateBegin((Integer) inv.getArgument(0));
                case "candidateEnd":
                    return v.candidateEnd((Integer) inv.getArgument(0));
                case "candidateResource":
                    return v.candidateResource((Integer) inv.getArgument(0));
                case "candidateRole":
                    return v.roleOf((Integer) inv.getArgument(0));
                default:
                    return null;
            }
        });
        List<String> pruned = new ArrayList<>();
        List<String> restricted = new ArrayList<>();
        PreprocessContext ctx = new PreprocessContext() {
            public CandidateIndexBuilder.Mutable candidates() {
                return mutable;
            }

            public int[] resolveTasks(ConstraintScope scope) {
                return v.resolveTasks(scope);
            }

            public int[] resolveResources(ConstraintScope scope) {
                return v.resolveResources(scope);
            }

            public Horizon horizon() {
                return p.horizon();
            }

            public void pruneCandidate(int candidate, String reasonCode, String constraintId) {
                pruned.add(candidate + ":" + reasonCode + ":" + constraintId);
            }

            public void restrictStart(int task, int lo, int hi) {
                restricted.add(task + ":" + lo + ":" + hi);
            }

            public void restrictStartDomain(int task, int[] runs) {
                throw new AssertionError("not expected");
            }
        };
        family.preprocess(ctx, c, FamilyKit.bind(family, c));
        assertEquals(List.of("0:5:5"), restricted);
        assertEquals(List.of("1:FIXED_ASSIGNMENT:fix"), pruned);   // t1 on m2 only; t2 is out of scope
    }
}
