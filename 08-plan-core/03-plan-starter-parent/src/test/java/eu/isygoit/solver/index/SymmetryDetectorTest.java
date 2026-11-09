package eu.isygoit.solver.index;

import eu.isygoit.config.PlanLimitsProperties;
import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.InitialAssignment;
import eu.isygoit.dto.request.Preference;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.index.IndexFixtures.ProblemB;
import eu.isygoit.solver.index.IndexFixtures.StubFamily;
import eu.isygoit.solver.spi.ModelCost;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static eu.isygoit.solver.index.IndexFixtures.compiled;
import static eu.isygoit.solver.index.IndexFixtures.ids;
import static eu.isygoit.solver.index.IndexFixtures.problem;
import static eu.isygoit.solver.index.IndexFixtures.resource;
import static eu.isygoit.solver.index.IndexFixtures.skills;
import static eu.isygoit.solver.index.IndexFixtures.task;
import static org.assertj.core.api.Assertions.assertThat;

class SymmetryDetectorTest {

    private static CandidateIndex build(ProblemB b, List<eu.isygoit.solver.CompiledConstraint> constraints) {
        return CandidateIndexBuilder.build(b.problem(), constraints, new PlanLimitsProperties(), null);
    }

    private static CandidateIndex build(ProblemB b) {
        return build(b, List.of());
    }

    @Test
    void identicalUnreferencedTasksShareAClassAndOthersAreUnique() {
        CandidateIndex ix = build(problem(100).resource(resource("r0")).resource(resource("r1"))
                .task(task("a").duration(10))
                .task(task("b").duration(10))
                .task(task("c").duration(20))
                .task(task("d").duration(10)));
        assertThat(ix.taskSymmetryClass).containsExactly(0, 0, -1, 0);
    }

    @Test
    void taskClassIdsAreDenseAndOrderedByTheirFirstMember() {
        CandidateIndex ix = build(problem(100).resource(resource("r0"))
                .task(task("a").duration(5)).task(task("b").duration(7))
                .task(task("c").duration(5)).task(task("d").duration(7)));
        assertThat(ix.taskSymmetryClass).containsExactly(0, 1, 0, 1);
    }

    @Test
    void tasksWithDifferentPrizeRequiredFlagWindowOrTagsAreNotInterchangeable() {
        ProblemB b = problem(100).resource(resource("r0"));
        b.task(task("a").duration(10)).task(task("b").duration(10).optional(5))
                .task(task("c").duration(10).release(b.at(5)))
                .task(task("d").duration(10).tags("x")).task(task("e").duration(10).tags("y"))
                .task(task("f").duration(10));
        CandidateIndex ix = build(b);
        assertThat(ix.taskSymmetryClass).containsExactly(0, -1, -1, -1, -1, 0);
    }

    @Test
    void pinnedPrecedenceAndPreferenceTasksAreExcluded() {
        ProblemB b = problem(100).resource(resource("r0")).resource(resource("r1"));
        b.task(task("a").duration(10)).task(task("b").duration(10))
                .task(task("pinned").duration(10).pin(b.at(5)))
                .task(task("p1").duration(10)).task(task("p2").duration(10).after("p1"))
                .task(task("pref").duration(10).preference(new Preference(Preference.Kind.RESOURCE, "r1", null, 3)))
                .task(task("x").duration(10)).task(task("y").duration(10));
        CandidateIndex ix = build(b);
        // a, b, x, y are interchangeable; pinned, p1 (predecessor), p2, pref are not
        assertThat(ix.taskSymmetryClass).containsExactly(0, 0, -1, -1, -1, -1, 0, 0);
    }

    @Test
    void tasksNamedByAScopeOrAParamsStringAreExcluded() {
        Constraint scoped = new Constraint("c1", "stub", IEnumEnforcement.Types.STRICTLY_REQUIRED,
                new ConstraintScope("task", "a"), Map.of(), null, 0, null);
        Constraint byParam = new Constraint("c2", "stub", IEnumEnforcement.Types.STRICTLY_REQUIRED, null,
                Map.of("pairs", List.of(Map.of("a", "b", "b", "c"))), null, 0, null);
        StubFamily family = new StubFamily("stub", null, ModelCost.ZERO);
        CandidateIndex ix = build(problem(100).resource(resource("r0"))
                        .task(task("a").duration(10)).task(task("b").duration(10)).task(task("c").duration(10))
                        .task(task("d").duration(10)).task(task("e").duration(10)),
                List.of(compiled(0, scoped, family), compiled(1, byParam, family)));
        // a (scope), b and c (params strings that equal task ids) are excluded; d and e remain interchangeable
        assertThat(ix.taskSymmetryClass).containsExactly(-1, -1, -1, 0, 0);
    }

    @Test
    void aTagPrefixInParamsExcludesTheTasksOfTheGroup() {
        Constraint grouped = new Constraint("g", "stub", IEnumEnforcement.Types.STRICTLY_REQUIRED, null,
                Map.of("groupByTagPrefix", "class-"), null, 0, null);
        CandidateIndex ix = build(problem(100).resource(resource("r0"))
                        .task(task("a").duration(10).tags("class-1")).task(task("b").duration(10).tags("class-1"))
                        .task(task("c").duration(10)).task(task("d").duration(10)),
                List.of(compiled(0, grouped, new StubFamily("stub", null, ModelCost.ZERO))));
        assertThat(ix.taskSymmetryClass).containsExactly(-1, -1, 0, 0);
    }

    @Test
    void initialSolutionTasksAreExcluded() {
        CandidateIndex ix = build(problem(100).resource(resource("r0")).resource(resource("r1"))
                .task(task("a").duration(10)).task(task("b").duration(10)).task(task("c").duration(10))
                .initial(new InitialAssignment("a", null, List.of("r0"), false)));
        assertThat(ix.taskSymmetryClass).containsExactly(-1, 0, 0);
        assertThat(ix.resourceSymmetryClass).containsExactly(-1, -1);
    }

    @Test
    void identicalResourcesNeverNamedByIdShareAClass() {
        CandidateIndex ix = build(problem(100)
                .resource(resource("r0").skills("a")).resource(resource("r1").skills("a"))
                .resource(resource("r2").skills("a").capacity(2)).resource(resource("r3").skills("b"))
                .task(task("t").duration(5).requires(skills("a"))));
        assertThat(ix.resourceSymmetryClass).containsExactly(0, 0, -1, -1);
    }

    @Test
    void resourcesNamedByIdInASelectorAreExcluded() {
        CandidateIndex ix = build(problem(100)
                .resource(resource("r0")).resource(resource("r1")).resource(resource("r2"))
                .task(task("t").duration(5).requires(ids("r0", "r1", "r2")))
                .task(task("u").duration(5).requires(skills())));
        // r0, r1, r2 are all named by id: nothing is interchangeable
        assertThat(ix.resourceSymmetryClass).containsExactly(-1, -1, -1);
    }

    @Test
    void resourcesWithDifferentCostsCalendarsOrTagsAreNotInterchangeable() {
        ProblemB b = problem(100);
        b.resource(resource("r0").costPerTick(1.0)).resource(resource("r1").costPerTick(2.0))
                .resource(resource("r2").tags("night")).resource(resource("r3").tags("night"))
                .resource(resource("r4").unavailable(b.at(10), b.at(20))).resource(resource("r5"));
        b.task(task("t").duration(5));
        CandidateIndex ix = build(b);
        assertThat(ix.resourceSymmetryClass).containsExactly(-1, -1, 0, 0, -1, -1);
    }
}
