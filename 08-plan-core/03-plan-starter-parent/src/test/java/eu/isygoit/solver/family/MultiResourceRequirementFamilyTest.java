package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.ResourceRequirement;
import eu.isygoit.dto.request.ResourceSelector;
import eu.isygoit.dto.request.Task;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.family.FamilyKit.Validation;
import eu.isygoit.solver.family.FamilyKit.View;
import eu.isygoit.solver.spi.ModelCost;
import eu.isygoit.solver.spi.ScopeKind;
import eu.isygoit.solver.spi.TaskRewriter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static eu.isygoit.solver.family.FamilyKit.map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiResourceRequirementFamilyTest {

    private final MultiResourceRequirementFamily family = new MultiResourceRequirementFamily();

    private Constraint constraint(ConstraintScope scope, Map<String, Object> params) {
        return FamilyKit.constraint("mr", "multi_resource_requirement", IEnumEnforcement.Types.STRICTLY_REQUIRED, scope, 0,
                params);
    }

    private Task rewrite(Task task, Constraint c) {
        return ((TaskRewriter) family).rewrite(task, c, FamilyKit.bind(family, c));
    }

    @Test
    void identityAndHardOnly() {
        assertEquals("multi_resource_requirement", family.type());
        assertInstanceOf(TaskRewriter.class, family);
        assertEquals(ScopeKind.TASKS, family.scopeKind());
        assertFalse(family.supportedEnforcements().contains(IEnumEnforcement.Types.SOFT));
        assertFalse(family.supportedEnforcements().contains(IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION));
    }

    @Test
    void implicitRequirementIsReplacedNotAppended() {
        Constraint c = constraint(null, map("role", "operator", "count", 2, "amount", 3,
                "selector", map("skills", List.of("weld"))));
        Task rewritten = rewrite(FamilyKit.task("t", 5), c);
        assertEquals(1, rewritten.requirements().size());
        ResourceRequirement r = rewritten.requirements().get(0);
        assertEquals("operator", r.role());
        assertEquals(2, r.count());
        assertEquals(3, r.amount());
        assertEquals(List.of("weld"), r.selector().skills());
        assertEquals(1, rewritten.effectiveRequirements().size());
    }

    @Test
    void defaultRoleReplacesTheImplicitDefault() {
        Constraint c = constraint(null, map("selector", map("resourceIds", List.of("m1"))));
        Task rewritten = rewrite(FamilyKit.task("t", 5), c);
        assertEquals(1, rewritten.requirements().size());
        assertEquals(ResourceRequirement.DEFAULT_ROLE, rewritten.requirements().get(0).role());
    }

    @Test
    void sameRoleIsReplacedInPlaceOtherRoleAppended() {
        Task base = FamilyKit.task("t", 5, List.of(
                new ResourceRequirement("a", ResourceSelector.all(), 1, null),
                new ResourceRequirement("b", ResourceSelector.all(), 1, null)));
        Task replaced = rewrite(base, constraint(null, map("role", "a", "count", 4)));
        assertEquals(List.of("a", "b"), replaced.requirements().stream().map(ResourceRequirement::role).toList());
        assertEquals(4, replaced.requirements().get(0).count());
        Task appended = rewrite(base, constraint(null, map("role", "c")));
        assertEquals(List.of("a", "b", "c"), appended.requirements().stream().map(ResourceRequirement::role).toList());
    }

    @Test
    void tasksOutsideTheScopeAreUntouched() {
        Constraint c = constraint(new ConstraintScope("tag", "night"), map("role", "operator"));
        Task day = FamilyKit.task("d", 5, "day");
        assertSame(day, rewrite(day, c));
        Task night = FamilyKit.task("n", 5, "night");
        assertEquals("operator", rewrite(night, c).requirements().get(0).role());
        Task one = rewrite(FamilyKit.task("x", 5), constraint(new ConstraintScope("task", "x"), map("role", "r")));
        assertEquals("r", one.requirements().get(0).role());
        assertEquals("x", one.id());
        assertEquals(5, one.duration());
    }

    @Test
    void validate() {
        Constraint bad = constraint(null, map("count", 0, "amount", 0,
                "selector", map("resourceIds", List.of("ghost"), "excludeResourceIds", List.of("ghost2"))));
        PlanningProblem p = FamilyKit.problem(FamilyKit.horizon(10, 60), "UTC", List.of(FamilyKit.resource("m1", 1)),
                List.of(FamilyKit.task("t", 1)), List.of(bad));
        Validation v = new Validation(View.of(p));
        family.validate(v, bad, FamilyKit.bind(family, bad));
        assertEquals(2, v.errors.stream().filter(e -> e.startsWith("INVALID_REQUIREMENT@")).count());
        assertEquals(2, v.errors.stream().filter(e -> e.startsWith("UNKNOWN_REFERENCE@")).count());

        Constraint all = constraint(null, map("selector", map("resourceIds", List.of("ALL"))));
        Validation ok = new Validation(View.of(p));
        family.validate(ok, all, FamilyKit.bind(family, all));
        assertTrue(ok.errors.isEmpty());
    }

    @Test
    void estimateIsZeroAndCheckerHasNothingToReport() {
        Constraint c = constraint(null, map("role", "r"));
        PlanningProblem p = FamilyKit.problem(FamilyKit.horizon(10, 60), "UTC", List.of(FamilyKit.resource("m1", 1)),
                List.of(FamilyKit.task("t", 1)), List.of(c));
        View v = View.of(p).allCandidates();
        assertEquals(ModelCost.ZERO, family.estimate(v, c, FamilyKit.bind(family, c)));
        assertEquals(0, FamilyKit.run(family, p, v, new FamilyKit.Solution(v).assign("t", 0, 1, "m1"), c).count());
    }
}
