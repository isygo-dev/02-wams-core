package eu.isygoit.dto.request;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.isygoit.dto.request.params.LoadMetric;
import eu.isygoit.dto.request.params.LoadObjectiveParams;
import eu.isygoit.dto.request.params.MaxConsecutiveParams;
import eu.isygoit.dto.request.params.MaxLoadPerPeriodParams;
import eu.isygoit.dto.request.params.MinRestParams;
import eu.isygoit.dto.request.params.NoOverlapParams;
import eu.isygoit.dto.request.params.PeriodSpec;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.util.ParamsBinder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Deserializes the five complete examples of the design (section 2.6) with FAIL_ON_UNKNOWN_PROPERTIES and checks
 * that they round-trip through the IR records.
 */
class PlanningRequestJsonTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private static PlanningRequest load(String name) throws IOException {
        try (InputStream in = PlanningRequestJsonTest.class.getResourceAsStream("/requests/" + name)) {
            assertNotNull(in, "missing test resource " + name);
            return MAPPER.readValue(in, PlanningRequest.class);
        }
    }

    private static void assertRoundTrip(PlanningRequest request) throws IOException {
        String json = MAPPER.writeValueAsString(request);
        PlanningRequest again = MAPPER.readValue(json, PlanningRequest.class);
        assertEquals(request, again);
        JsonNode first = MAPPER.readTree(json);
        assertFalse(first.has("tenant"));
        JsonNode second = MAPPER.readTree(MAPPER.writeValueAsString(again));
        assertEquals(first, second);
    }

    @Test
    void schoolTimetable() throws IOException {
        PlanningRequest r = load("a-school-timetable.json");
        assertEquals("2.0", r.meta().version());
        assertEquals("Europe/Paris", r.meta().timezone());
        assertEquals(1, r.domain().resourceTypes().size());
        assertEquals(List.of("classId", "subject"), r.domain().taskTypes().get(0).attributes());
        assertEquals(900, r.horizon().tickSeconds());
        assertEquals(105L, r.horizon().horizonTicks());
        assertEquals(4, r.slots().size());
        assertEquals("Tue 08:00", r.slots().get(2).label());
        assertEquals(1.0, r.slots().get(2).cost());
        assertEquals(2, r.resources().size());
        assertTrue(r.resources().get(0).skills().contains("math"));
        assertEquals(4, r.tasks().size());
        Task t = r.tasks().get(0);
        assertEquals("7a-math", t.id());
        assertNull(t.duration());
        assertEquals(List.of("mon-p1", "tue-p1"), t.startSlots());
        assertTrue(t.tags().contains("class-7A"));
        assertEquals("teacher", t.requirements().get(0).role());
        assertEquals(List.of("math"), t.requirements().get(0).selector().skills());
        assertEquals(1, t.requirements().get(0).count());
        assertEquals(1, r.constraints().size());
        Constraint c = r.constraints().get(0);
        assertEquals(IEnumEnforcement.Types.STRICTLY_REQUIRED, c.enforcement());
        assertEquals("all", c.scope().kind());
        NoOverlapParams p = ParamsBinder.bind(c.params(), NoOverlapParams.class);
        assertEquals("class-", p.groupByTagPrefix());
        assertEquals(0, p.minGapTicks());
        assertEquals("minimize_slot_cost", r.objectives().get(0).type());
        assertEquals(1, r.objectives().get(0).weight());
        assertEquals("cp_sat", r.solverOptions().engine());
        assertEquals(10, r.solverOptions().maxTimeSeconds());
        assertEquals(2, r.solverOptions().numWorkers());
        assertEquals(0L, r.solverOptions().seed());
        assertNull(r.initialSolution());
        assertRoundTrip(r);
    }

    @Test
    void jobShop() throws IOException {
        PlanningRequest r = load("b-job-shop.json");
        assertEquals(480L, r.horizon().horizonTicks());
        assertEquals(60, r.horizon().tickSeconds());
        assertEquals(2, r.resources().size());
        assertEquals(1, r.resources().get(0).capacity());
        assertEquals(6, r.tasks().size());
        Task a2 = r.tasks().get(1);
        assertEquals(20, a2.duration());
        PrecedenceRef a1 = a2.predecessors().get(0);
        assertEquals("A1", a1.taskId());
        assertEquals(PrecedenceRef.Type.FINISH_START, a1.type());
        assertEquals(0, a1.minLag());
        assertNull(a1.maxLag());
        Task b2 = r.tasks().get(3);
        PrecedenceRef b1 = b2.predecessors().get(0);
        assertEquals(PrecedenceRef.Type.FINISH_START, b1.type());
        assertEquals(PrecedenceRef.Presence.WHEN_BOTH, b1.presence());
        assertEquals(List.of("M2"), r.tasks().get(2).requirements().get(0).selector().resourceIds());
        assertTrue(r.solverOptions().deterministic());
        assertEquals(4, r.solverOptions().numWorkers());
        assertTrue(r.slots().isEmpty());
        assertTrue(r.constraints().isEmpty());
        assertTrue(r.domain().resourceTypes().isEmpty());
        assertRoundTrip(r);
    }

    @Test
    void shiftRoster() throws IOException {
        PlanningRequest r = load("c-shift-roster.json");
        assertEquals(72L, r.horizon().horizonTicks());
        Resource cleo = r.resources().get(2);
        assertEquals(1, cleo.unavailable().size());
        assertEquals(Instant.parse("2026-09-07T16:00:00Z"), cleo.unavailable().get(0).end());
        assertEquals(5, r.tasks().size());
        Task night = r.tasks().get(3);
        assertEquals(Instant.parse("2026-09-08T22:00:00Z"), night.pin().start());
        assertTrue(night.pin().resourceIds().isEmpty());
        assertEquals(List.of("nurse", "icu"), night.requirements().get(0).selector().skills());
        assertEquals(3, r.constraints().size());
        MinRestParams rest = ParamsBinder.bind(r.constraints().get(0).params(), MinRestParams.class);
        assertEquals(11, rest.minRestTicks());
        assertNull(rest.afterTasks());
        MaxLoadPerPeriodParams load = ParamsBinder.bind(r.constraints().get(1).params(), MaxLoadPerPeriodParams.class);
        assertEquals(PeriodSpec.Unit.DAY, load.period().unit());
        assertEquals(LoadMetric.TICKS, load.metric());
        assertEquals(8, load.max());
        Constraint soft = r.constraints().get(2);
        assertEquals(IEnumEnforcement.Types.SOFT, soft.enforcement());
        assertEquals(10, soft.weight());
        assertEquals(0, soft.tier());
        MaxConsecutiveParams consecutive = ParamsBinder.bind(soft.params(), MaxConsecutiveParams.class);
        assertEquals(2, consecutive.max());
        LoadObjectiveParams fair = ParamsBinder.bind(r.objectives().get(0).params(), LoadObjectiveParams.class);
        assertEquals(LoadMetric.TICKS, fair.metric());
        assertEquals(0, r.objectives().get(0).tier());
        assertRoundTrip(r);
    }

    @Test
    void projectPlan() throws IOException {
        PlanningRequest r = load("d-project-plan.json");
        assertEquals(20L, r.horizon().horizonTicks());
        assertEquals(86_400, r.horizon().tickSeconds());
        assertEquals(2, r.resources().get(0).capacity());
        assertEquals(5, r.tasks().size());
        assertEquals(2, r.tasks().get(2).demand());
        assertEquals(1, r.tasks().get(0).demand());
        assertEquals(2, r.tasks().get(3).predecessors().size());
        Task e = r.tasks().get(4);
        assertEquals(Instant.parse("2026-09-13T00:00:00Z"), e.deadline());
        assertEquals(0, e.predecessors().get(0).minLag());
        assertEquals(12, r.horizon().toTick(e.deadline()));
        assertEquals("Heavy digging", r.tasks().get(2).name());
        assertEquals("minimize_makespan", r.objectives().get(0).type());
        assertEquals(2, r.solverOptions().numWorkers());
        assertFalse(r.solverOptions().deterministic());
        assertRoundTrip(r);
    }

    @Test
    void prizeCollecting() throws IOException {
        PlanningRequest r = load("e-prize-collecting.json");
        assertEquals(480L, r.horizon().horizonTicks());
        assertEquals(5, r.tasks().size());
        for (Task task : r.tasks()) {
            assertFalse(task.requiredTask());
            assertTrue(task.requirements().isEmpty());
            assertEquals(1, task.effectiveRequirements().size());
            assertEquals("default", task.effectiveRequirements().get(0).role());
        }
        assertEquals(50L, r.tasks().get(0).prize());
        assertEquals(90L, r.tasks().get(2).prize());
        assertEquals(240, r.tasks().get(2).duration());
        assertTrue(r.constraints().isEmpty());
        assertTrue(r.objectives().isEmpty());
        assertEquals("tech-1", r.resources().get(0).id());
        assertEquals("cp_sat", r.solverOptions().engine());
        assertEquals(Boolean.TRUE, r.solverOptions().explainInfeasibility());
        assertRoundTrip(r);
    }

    @Test
    void unknownPropertyFails() {
        String json = "{\"taksks\":[]}";
        assertThrows(UnrecognizedPropertyException.class, () -> MAPPER.readValue(json, PlanningRequest.class));
        assertThrows(UnrecognizedPropertyException.class,
                () -> MAPPER.readValue("{\"tenant\":\"t\"}", PlanningRequest.class));
        String nested = "{\"horizon\":{\"start\":\"2026-09-07T08:00:00Z\",\"end\":\"2026-09-07T16:00:00Z\","
                + "\"tickSeconds\":60,\"tickSecond\":1}}";
        assertThrows(UnrecognizedPropertyException.class, () -> MAPPER.readValue(nested, PlanningRequest.class));
    }

    @Test
    void minimalRequestGetsDefaults() throws IOException {
        PlanningRequest r = MAPPER.readValue("{}", PlanningRequest.class);
        assertNull(r.horizon());
        assertNull(r.meta());
        assertNull(r.initialSolution());
        assertNotNull(r.domain());
        assertTrue(r.slots().isEmpty());
        assertTrue(r.calendars().isEmpty());
        assertTrue(r.setupMatrices().isEmpty());
        assertTrue(r.resources().isEmpty());
        assertTrue(r.tasks().isEmpty());
        assertEquals(SolverOptions.defaults(), r.solverOptions());
    }

    @Test
    void calendarsSetupsAndInitialSolutionBind() throws IOException {
        String json = "{"
                + "\"horizon\":{\"start\":\"2026-09-07T00:00:00Z\",\"end\":\"2026-09-08T00:00:00Z\",\"tickSeconds\":60},"
                + "\"calendars\":[{\"id\":\"day\",\"timezone\":\"Europe/Paris\","
                + "\"windows\":[{\"start\":\"2026-09-07T06:00:00Z\",\"end\":\"2026-09-07T10:00:00Z\"}],"
                + "\"recurring\":[{\"days\":[\"MONDAY\",\"TUESDAY\"],\"from\":\"08:00\",\"to\":\"12:00\","
                + "\"validFrom\":\"2026-09-01\"}],"
                + "\"breaks\":[{\"from\":\"12:00\",\"to\":\"13:00\"}]}],"
                + "\"setupMatrices\":[{\"id\":\"m\",\"defaultTicks\":5,\"entries\":[{\"from\":\"a\",\"to\":\"b\",\"ticks\":9}]}],"
                + "\"initialSolution\":{\"assignments\":[{\"taskId\":\"t\",\"start\":\"2026-09-07T06:00:00Z\","
                + "\"resourceIds\":[\"r\"],\"locked\":true}]}}";
        PlanningRequest r = MAPPER.readValue(json, PlanningRequest.class);
        Calendar cal = r.calendars().get(0);
        assertEquals(1, cal.windows().size());
        assertEquals(2, cal.recurring().get(0).days().size());
        assertEquals("08:00", cal.recurring().get(0).from().toString());
        assertNull(cal.recurring().get(0).validTo());
        assertTrue(cal.breaks().get(0).days().isEmpty());
        assertEquals(5, r.setupMatrices().get(0).defaultTicks());
        assertEquals(0, r.setupMatrices().get(0).sameClassTicks());
        assertEquals(9, r.setupMatrices().get(0).entries().get(0).ticks());
        assertTrue(r.initialSolution().assignments().get(0).locked());
        assertRoundTrip(r);
    }

    @Test
    void objectiveConvenienceConstructorIsNotPickedByJackson() throws IOException {
        Objective o = MAPPER.readValue("{\"id\":\"o\",\"type\":\"minimize_makespan\",\"weight\":3,\"tier\":2,\"scale\":10}",
                Objective.class);
        assertEquals(3, o.weight());
        assertEquals(2, o.tier());
        assertEquals(10, o.scale());
        assertEquals("all", o.scope().kind());
        assertTrue(o.params().isEmpty());
    }
}
