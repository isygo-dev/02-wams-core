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
import java.util.Map;

import static eu.isygoit.solver.family.FamilyKit.map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForbiddenPairFamilyTest {

    private final ForbiddenPairFamily family = new ForbiddenPairFamily();

    private Constraint constraint(Map<String, Object> params) {
        return FamilyKit.soft("fp", "forbidden_pair", 4, params);
    }

    private Map<String, Object> pair(String a, String b) {
        return map("a", a, "b", b);
    }

    private PlanningProblem problem(Constraint c) {
        return FamilyKit.problem(FamilyKit.horizon(40, 3600), "UTC",
                List.of(FamilyKit.resource("r1", 2), FamilyKit.resource("r2", 2), FamilyKit.resource("r3", 2)),
                List.of(FamilyKit.task("a", 4), FamilyKit.task("b", 4), FamilyKit.task("c", 4)), List.of(c));
    }

    @Test
    void identity() {
        assertEquals("forbidden_pair", family.type());
        assertEquals(ScopeKind.NONE, family.scopeKind());
        assertTrue(family.supportedEnforcements().contains(IEnumEnforcement.Types.SOFT));
    }

    @Test
    void validate() {
        Constraint noKind = constraint(map("pairs", List.of(pair("a", "b"))));
        Validation v = new Validation(View.of(problem(noKind)));
        family.validate(v, noKind, FamilyKit.bind(family, noKind));
        assertTrue(v.hasError("MISSING_REQUIRED_FIELD"));

        Constraint noPairs = constraint(map("kind", "TASK_TASK"));
        Validation w = new Validation(View.of(problem(noPairs)));
        family.validate(w, noPairs, FamilyKit.bind(family, noPairs));
        assertTrue(w.hasError("MISSING_REQUIRED_FIELD"));

        Constraint bad = constraint(map("kind", "TASK_TASK", "pairs", List.of(pair("a", "a"), pair("a", "ghost"))));
        Validation x = new Validation(View.of(problem(bad)));
        family.validate(x, bad, FamilyKit.bind(family, bad));
        assertTrue(x.hasError("PARAMS_INVALID"));
        assertTrue(x.hasError("UNKNOWN_REFERENCE"));

        Constraint resourcesAreChecked = constraint(map("kind", "RESOURCE_RESOURCE", "pairs", List.of(pair("r1", "a"))));
        Validation y = new Validation(View.of(problem(resourcesAreChecked)));
        family.validate(y, resourcesAreChecked, FamilyKit.bind(family, resourcesAreChecked));
        assertTrue(y.hasError("UNKNOWN_REFERENCE"));

        Constraint ok = constraint(map("kind", "RESOURCE_RESOURCE", "pairs", List.of(pair("r1", "r2"))));
        Validation z = new Validation(View.of(problem(ok)));
        family.validate(z, ok, FamilyKit.bind(family, ok));
        assertTrue(z.errors.isEmpty());
    }

    @Test
    void estimate() {
        Constraint tasks = constraint(map("kind", "TASK_TASK", "pairs", List.of(pair("a", "b"), pair("a", "c"))));
        PlanningProblem p = problem(tasks);
        View v = View.of(p).allCandidates();
        assertEquals(2, family.estimate(v, tasks, FamilyKit.bind(family, tasks)).pairwise());
        Constraint res = constraint(map("kind", "RESOURCE_RESOURCE", "pairs", List.of(pair("r1", "r2"))));
        assertEquals(3, family.estimate(v, res, FamilyKit.bind(family, res)).constraints());
        assertEquals(ModelCost.ZERO.pairwise(), 0);
    }

    @Test
    void sameResourceIsOneViolationPerPair() {
        Constraint c = constraint(map("kind", "TASK_TASK", "relation", "SAME_RESOURCE",
                "pairs", List.of(pair("a", "b"), pair("a", "c"))));
        PlanningProblem p = problem(c);
        View v = View.of(p).allCandidates();
        // a and b share BOTH r1 and r2: still one violation; c is elsewhere
        Solution s = new Solution(v).assign("a", 0, 4, "r1", "r2").assign("b", 10, 14, "r1", "r2").assign("c", 20, 24, "r3");
        Sink sink = FamilyKit.run(family, p, v, s, c);
        assertEquals(1, sink.count());
        assertEquals(1, sink.measure());
        // the default relation is SAME_RESOURCE
        Constraint dflt = constraint(map("kind", "TASK_TASK", "pairs", List.of(pair("a", "b"))));
        assertEquals(1, FamilyKit.run(family, p, v, s, dflt).count());
    }

    @Test
    void timeOverlapUsesHalfOpenIntervals() {
        Constraint c = constraint(map("kind", "TASK_TASK", "relation", "TIME_OVERLAP", "pairs", List.of(pair("a", "b"))));
        PlanningProblem p = problem(c);
        View v = View.of(p).allCandidates();
        assertEquals(1, FamilyKit.run(family, p, v, new Solution(v).assign("a", 0, 4, "r1").assign("b", 3, 7, "r2"), c).count());
        assertEquals(0, FamilyKit.run(family, p, v, new Solution(v).assign("a", 0, 4, "r1").assign("b", 4, 8, "r1"), c).count());
        assertEquals(0, FamilyKit.run(family, p, v, new Solution(v).assign("a", 0, 4, "r1"), c).count());
    }

    @Test
    void resourcePairIsOneViolationPerTask() {
        Constraint c = constraint(map("kind", "RESOURCE_RESOURCE", "pairs", List.of(pair("r1", "r2"))));
        PlanningProblem p = problem(c);
        View v = View.of(p).allCandidates();
        Solution s = new Solution(v).assign("a", 0, 4, "r1", "r2").assign("b", 5, 9, "r1", "r3")
                .assign("c", 10, 14, "r2", "r1");
        Sink sink = FamilyKit.run(family, p, v, s, c);
        assertEquals(2, sink.count());     // tasks a and c have both resources
        assertEquals(2, sink.measure());
    }
}
