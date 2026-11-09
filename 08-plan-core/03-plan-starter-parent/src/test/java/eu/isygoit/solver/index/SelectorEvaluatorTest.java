package eu.isygoit.solver.index;

import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.ResourceSelector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static eu.isygoit.solver.index.IndexFixtures.resource;
import static org.assertj.core.api.Assertions.assertThat;

class SelectorEvaluatorTest {

    private SelectorEvaluator evaluator;

    @BeforeEach
    void setUp() {
        List<Resource> resources = List.of(
                resource("r0").type("t1").skills("a", "b").tags("x").build(),
                resource("r1").type("t1").skills("a").build(),
                resource("r2").type("t2").skills("b").tags("x").build());
        evaluator = new SelectorEvaluator(resources);
    }

    private static ResourceSelector selector(List<String> ids, List<String> types, List<String> skills,
                                             List<String> anySkills, List<String> tags, List<String> exclude) {
        return new ResourceSelector(ids, types, skills, anySkills, tags, exclude);
    }

    @Test
    void emptySelectorSelectsEveryResource() {
        assertThat(evaluator.resources(ResourceSelector.all())).containsExactly(0, 1, 2);
        assertThat(evaluator.resources(null)).containsExactly(0, 1, 2);
    }

    @Test
    void skillsMustAllMatch() {
        assertThat(evaluator.resources(selector(null, null, List.of("a"), null, null, null))).containsExactly(0, 1);
        assertThat(evaluator.resources(selector(null, null, List.of("a", "b"), null, null, null))).containsExactly(0);
        assertThat(evaluator.resources(selector(null, null, List.of("nope"), null, null, null))).isEmpty();
    }

    @Test
    void anySkillsNeedsAtLeastOne() {
        assertThat(evaluator.resources(selector(null, null, null, List.of("a", "zzz"), null, null)))
                .containsExactly(0, 1);
        assertThat(evaluator.resources(selector(null, null, null, List.of("b"), null, null))).containsExactly(0, 2);
    }

    @Test
    void typesAreOrTagsAreAnd() {
        assertThat(evaluator.resources(selector(null, List.of("t2", "t1"), null, null, null, null)))
                .containsExactly(0, 1, 2);
        assertThat(evaluator.resources(selector(null, null, null, null, List.of("x"), null))).containsExactly(0, 2);
        assertThat(evaluator.resources(selector(null, List.of("t1"), null, null, List.of("x"), null)))
                .containsExactly(0);
    }

    @Test
    void idsRestrictAndTheLiteralAllIsAWildcard() {
        assertThat(evaluator.resources(selector(List.of("r2", "r1"), null, null, null, null, null)))
                .containsExactly(1, 2);
        assertThat(evaluator.resources(selector(List.of("ALL"), null, null, null, null, null)))
                .containsExactly(0, 1, 2);
        assertThat(evaluator.resources(selector(List.of("ghost"), null, null, null, null, null))).isEmpty();
    }

    @Test
    void excludeIsAppliedLast() {
        assertThat(evaluator.resources(selector(null, null, List.of("a"), null, null, List.of("r0"))))
                .containsExactly(1);
    }

    @Test
    void equalSelectorsAreEvaluatedOnce() {
        ResourceSelector first = selector(null, null, List.of("a"), null, null, null);
        ResourceSelector equalCopy = selector(null, null, List.of("a"), null, null, null);
        assertThat(first).isNotSameAs(equalCopy);
        assertThat(evaluator.evaluate(first)).isSameAs(evaluator.evaluate(equalCopy));
        assertThat(evaluator.distinctSelectors()).isEqualTo(1);
        assertThat(evaluator.cardinality(first)).isEqualTo(2);
        assertThat(evaluator.resourceIdx("r2")).isEqualTo(2);
        assertThat(evaluator.resourceIdx("ghost")).isEqualTo(-1);
    }
}
