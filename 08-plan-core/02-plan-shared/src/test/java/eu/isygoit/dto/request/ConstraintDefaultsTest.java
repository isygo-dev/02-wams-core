package eu.isygoit.dto.request;

import eu.isygoit.dto.request.params.LoadMetric;
import eu.isygoit.dto.request.params.MaxLoadPerPeriodParams;
import eu.isygoit.dto.request.params.NoOverlapParams;
import eu.isygoit.dto.request.params.PlanChangeParams;
import eu.isygoit.enums.IEnumEnforcement;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Defaults and immutability applied by the compact constructors of the request IR.
 */
class ConstraintDefaultsTest {

    private static Constraint constraint(IEnumEnforcement.Types enforcement, int weight, Integer tier) {
        return new Constraint("c", "min_rest", enforcement, null, null, null, weight, tier);
    }

    @Test
    void constraintDefaults() {
        Constraint c = constraint(null, 0, null);
        assertEquals(IEnumEnforcement.Types.STRICTLY_REQUIRED, c.enforcement());
        assertEquals("all", c.scope().kind());
        assertNull(c.scope().value());
        assertTrue(c.params().isEmpty());
        assertNull(c.relaxation());
        assertEquals(0, c.weight());
        assertEquals(0, c.tier());
    }

    @Test
    void softConstraintWithZeroWeightGetsWeightOne() {
        assertEquals(1, constraint(IEnumEnforcement.Types.SOFT, 0, null).weight());
        assertEquals(10, constraint(IEnumEnforcement.Types.SOFT, 10, null).weight());
        assertEquals(0, constraint(IEnumEnforcement.Types.CONFIGURABLE_HARD, 0, null).weight());
        assertEquals(0, constraint(IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION, 0, null).weight());
        assertEquals(0, constraint(IEnumEnforcement.Types.STRICTLY_REQUIRED, 0, null).weight());
    }

    @Test
    void constraintTierIsKept() {
        assertEquals(3, constraint(IEnumEnforcement.Types.SOFT, 2, 3).tier());
        assertEquals(0, constraint(IEnumEnforcement.Types.SOFT, 2, null).tier());
    }

    @Test
    void negativeWeightsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> constraint(IEnumEnforcement.Types.SOFT, -1, null));
        assertThrows(IllegalArgumentException.class, () -> new Objective("o", "minimize_makespan", -1));
    }

    @Test
    void constraintParamsAreCopiedAndImmutable() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("minRestTicks", 11);
        Constraint c = new Constraint("c", "min_rest", null, null, source, null, 0, null);
        source.put("later", 1);
        assertEquals(Map.of("minRestTicks", 11), c.params());
        assertThrows(UnsupportedOperationException.class, () -> c.params().put("x", 1));
    }

    @Test
    void scopeDefaults() {
        assertEquals("all", new ConstraintScope(null, null).kind());
        assertEquals("all", new ConstraintScope(" ", "ignored").kind());
        assertEquals("tag", new ConstraintScope("tag", "class-7A").kind());
        assertEquals(ConstraintScope.all(), new ConstraintScope("all", null));
    }

    @Test
    void relaxationPolicyIsUnchanged() {
        RelaxationPolicy p = new RelaxationPolicy(true, 3, 1.0);
        assertTrue(p.authorized());
        assertThrows(IllegalArgumentException.class, () -> new RelaxationPolicy(true, -1, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new RelaxationPolicy(true, 1, 0.5));
        assertThrows(IllegalArgumentException.class, () -> new RelaxationPolicy(true, 1, Double.NaN));
    }

    @Test
    void objectiveDefaults() {
        Objective o = new Objective("makespan", "minimize_makespan", 1);
        assertEquals(0, o.tier());
        assertNull(o.scale());
        assertEquals(ConstraintScope.all(), o.scope());
        assertTrue(o.params().isEmpty());
        Objective canonical = new Objective("o", "t", 0, null, null, null, null);
        assertEquals(0, canonical.weight());
        assertEquals(0, canonical.tier());
        assertEquals(-1, new Objective("o", "t", 1, -1, 5, null, null).tier());
        assertEquals(5, new Objective("o", "t", 1, -1, 5, null, null).scale());
    }

    @Test
    void precedenceRefDefaults() {
        PrecedenceRef r = new PrecedenceRef("A1", null, null, null, null);
        assertEquals(PrecedenceRef.Type.FINISH_START, r.type());
        assertEquals(0, r.minLag());
        assertNull(r.maxLag());
        assertEquals(PrecedenceRef.Presence.WHEN_BOTH, r.presence());
        assertEquals(-2, new PrecedenceRef("A1", null, -2, 5, PrecedenceRef.Presence.IMPLIES).minLag());
    }

    @Test
    void requirementDefaults() {
        ResourceRequirement r = new ResourceRequirement(null, null, null, null);
        assertEquals("default", r.role());
        assertEquals(ResourceSelector.all(), r.selector());
        assertEquals(1, r.count());
        assertNull(r.amount());
        assertEquals(r, ResourceRequirement.implicit());
        assertEquals("teacher", new ResourceRequirement("teacher", null, 2, 3).role());
        assertEquals(2, new ResourceRequirement("teacher", null, 2, 3).count());
        assertEquals(3, new ResourceRequirement("teacher", null, 2, 3).amount());
    }

    @Test
    void selectorListsDefaultToEmpty() {
        ResourceSelector s = ResourceSelector.all();
        assertTrue(s.resourceIds().isEmpty());
        assertTrue(s.resourceTypeIds().isEmpty());
        assertTrue(s.skills().isEmpty());
        assertTrue(s.anySkills().isEmpty());
        assertTrue(s.tags().isEmpty());
        assertTrue(s.excludeResourceIds().isEmpty());
    }

    @Test
    void taskDefaults() {
        Task t = new Task("t", null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null);
        assertEquals(1, t.demand());
        assertEquals(0, t.priority());
        assertEquals(Boolean.TRUE, t.required());
        assertTrue(t.requiredTask());
        assertNull(t.duration());
        assertNull(t.prize());
        assertEquals(1L, t.effectivePrize());
        assertTrue(t.requirements().isEmpty());
        assertEquals(List.of(ResourceRequirement.implicit()), t.effectiveRequirements());
        assertTrue(t.startSlots().isEmpty());
        assertTrue(t.predecessors().isEmpty());
        assertTrue(t.tags().isEmpty());
        assertTrue(t.preferences().isEmpty());
        assertTrue(t.resourceDurations().isEmpty());
        assertTrue(t.attributes().isEmpty());
        assertNull(t.pin());
        assertNull(t.release());
        assertNull(t.deadline());
        assertNull(t.dueDate());
        assertNull(t.setupClass());
    }

    @Test
    void optionalTaskAndPrize() {
        Task t = new Task("t", null, null, 5, null, 2, 3, Boolean.FALSE, 90L, null, null, null, null, null,
                null, null, null, null, null, null);
        assertFalse(t.requiredTask());
        assertEquals(90L, t.effectivePrize());
        assertEquals(2, t.demand());
        assertEquals(3, t.priority());
    }

    @Test
    void taskCollectionsAreCopiedAndImmutable() {
        List<String> slots = new ArrayList<>(List.of("s1"));
        Task t = new Task("t", null, null, 5, null, null, null, null, null, null, null, null, null, slots, null,
                null, null, Set.of("b", "a"), null, null);
        slots.add("s2");
        assertEquals(List.of("s1"), t.startSlots());
        assertThrows(UnsupportedOperationException.class, () -> t.startSlots().add("x"));
        assertThrows(UnsupportedOperationException.class, () -> t.tags().add("x"));
        assertThrows(UnsupportedOperationException.class, () -> t.attributes().put("x", 1));
        assertEquals(Set.of("a", "b"), t.tags());
    }

    @Test
    void taskTagsKeepInsertionOrder() {
        Set<String> ordered = new java.util.LinkedHashSet<>(List.of("z", "m", "a"));
        Task t = new Task("t", null, null, 1, null, null, null, null, null, null, null, null, null, null, null,
                null, null, ordered, null, null);
        assertEquals(List.of("z", "m", "a"), new ArrayList<>(t.tags()));
    }

    @Test
    void resourceDefaults() {
        Resource r = new Resource("r", null, null, null, null, null, null, null, null, null, null, null, null);
        assertEquals(1, r.capacity());
        assertTrue(r.skills().isEmpty());
        assertTrue(r.tags().isEmpty());
        assertTrue(r.unavailable().isEmpty());
        assertEquals(0.0, r.costPerTick());
        assertEquals(0.0, r.costPerUse());
        assertNull(r.maxLoadTicks());
        assertNull(r.calendarId());
        assertNull(r.setupMatrixId());
        assertTrue(r.attributes().isEmpty());
        assertEquals(4, new Resource("r", null, null, 4, null, null, null, null, 1.5, 2.5, 100, "m", null).capacity());
    }

    @Test
    void calendarAndRecurringDefaults() {
        Calendar c = new Calendar("c", null, null, null, null, null, null);
        assertTrue(c.windows().isEmpty());
        assertTrue(c.recurring().isEmpty());
        assertTrue(c.exceptions().isEmpty());
        assertTrue(c.breaks().isEmpty());
        assertNull(c.timezone());
        RecurringWindow w = new RecurringWindow(null, null, null, null, null);
        assertTrue(w.days().isEmpty());
        RecurringWindow weekdays = new RecurringWindow(
                Set.of(DayOfWeek.FRIDAY, DayOfWeek.MONDAY), null, null, null, null);
        assertEquals(List.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), new ArrayList<>(weekdays.days()),
                "days keep declaration order of the enum");
    }

    @Test
    void setupAndPinAndInitialSolutionDefaults() {
        SetupMatrix m = new SetupMatrix("m", 0, 0, null);
        assertTrue(m.entries().isEmpty());
        assertTrue(new Pin(null, null).resourceIds().isEmpty());
        assertTrue(new InitialAssignment("t", null, null, false).resourceIds().isEmpty());
        assertTrue(new InitialSolution(null).assignments().isEmpty());
    }

    @Test
    void metaTimezoneDefaultsToUtc() {
        assertEquals("UTC", new PlanningMeta(null, "2.0", null, null, null).timezone());
        assertEquals("UTC", new PlanningMeta(null, "2.0", null, null, "  ").timezone());
        assertEquals("Europe/Paris", new PlanningMeta(null, "2.0", null, null, "Europe/Paris").timezone());
        assertNull(new PlanningMeta(null, null, null, null, null).version());
        assertThrows(IllegalArgumentException.class,
                () -> new PlanningMeta("x".repeat(PlanningMeta.CLIENT_REF_MAX_LENGTH + 1), "2.0", null, null, null));
        assertEquals("2.0", PlanningMeta.SCHEMA_VERSION);
    }

    @Test
    void planningRequestAndProblemDefaults() {
        PlanningRequest r = new PlanningRequest(null, null, null, null, null, null, null, null, null, null, null,
                null, null);
        assertNotNull(r.domain());
        assertTrue(r.domain().taskTypes().isEmpty());
        assertEquals(SolverOptions.defaults(), r.solverOptions());
        assertNull(r.initialSolution());
        PlanningProblem p = new PlanningProblem(null, null, null, null, null, null, null, null, null, null, null,
                null, null);
        assertEquals("UTC", p.timezone());
        assertEquals(SolverOptions.defaults(), p.options());
        assertNotNull(p.initialSolution());
        assertTrue(p.initialSolution().assignments().isEmpty());
        assertTrue(p.tasks().isEmpty());
    }

    @Test
    void paramsRecordDefaults() {
        assertEquals(0, new NoOverlapParams("class-", null).minGapTicks());
        assertEquals(LoadMetric.TICKS, new MaxLoadPerPeriodParams(null, null, 8, null).metric());
        assertEquals(1, new PlanChangeParams(null, 20).startDeviationWeight());
        assertEquals(20, new PlanChangeParams(null, 20).resourceChangeWeight());
    }

    @Test
    void slotAttributesAreImmutable() {
        Slot s = new Slot("s", null, null, null, null, 1.0, Map.of("k", "v"));
        assertThrows(UnsupportedOperationException.class, () -> s.attributes().put("x", 1));
        assertTrue(new Slot("s", null, null, null, null, 0.0, null).attributes().isEmpty());
    }
}
