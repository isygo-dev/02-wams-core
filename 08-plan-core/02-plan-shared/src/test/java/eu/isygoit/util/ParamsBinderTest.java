package eu.isygoit.util;

import eu.isygoit.dto.request.PrecedenceRef;
import eu.isygoit.dto.request.params.FixedAssignmentParams;
import eu.isygoit.dto.request.params.ForbiddenPairParams;
import eu.isygoit.dto.request.params.LoadMetric;
import eu.isygoit.dto.request.params.LoadObjectiveParams;
import eu.isygoit.dto.request.params.MaxConsecutiveParams;
import eu.isygoit.dto.request.params.MaxLoadPerPeriodParams;
import eu.isygoit.dto.request.params.MinRestParams;
import eu.isygoit.dto.request.params.PeriodSpec;
import eu.isygoit.dto.request.params.PlanChangeParams;
import eu.isygoit.dto.request.params.PrecedenceParams;
import eu.isygoit.dto.request.params.ResourceCapacityParams;
import eu.isygoit.dto.request.params.TaskEligibilityParams;
import eu.isygoit.dto.request.params.TardinessParams;
import eu.isygoit.dto.request.params.TimeWindowParams;
import eu.isygoit.exception.ParamsInvalidException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParamsBinderTest {

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    @Test
    void bindsASimpleRecord() {
        MinRestParams p = ParamsBinder.bind(map("minRestTicks", 11), MinRestParams.class);
        assertEquals(11, p.minRestTicks());
        assertNull(p.afterTasks());
    }

    @Test
    void bindsNestedRecordsAndEnums() {
        MaxConsecutiveParams p = ParamsBinder.bind(
                map("period", map("unit", "TICKS", "ticks", 96), "max", 2), MaxConsecutiveParams.class);
        assertEquals(PeriodSpec.Unit.TICKS, p.period().unit());
        assertEquals(96, p.period().ticks());
        assertEquals(2, p.max());

        MinRestParams withScope = ParamsBinder.bind(
                map("minRestTicks", 4, "afterTasks", map("kind", "tag", "value", "night")), MinRestParams.class);
        assertEquals("tag", withScope.afterTasks().kind());
        assertEquals("night", withScope.afterTasks().value());

        PrecedenceParams prec = ParamsBinder.bind(map(
                "from", map("kind", "tag", "value", "cut"), "to", map("kind", "tag", "value", "weld"),
                "type", "START_START", "minLag", 3, "pairing", "BY_ORDER"), PrecedenceParams.class);
        assertEquals(PrecedenceRef.Type.START_START, prec.type());
        assertEquals(PrecedenceParams.PairingMode.BY_ORDER, prec.pairing());
        assertEquals(3, prec.minLag());
        assertNull(prec.maxLag());
    }

    @Test
    void bindsInstantsAndLists() {
        TimeWindowParams tw = ParamsBinder.bind(
                map("earliestStart", "2026-09-07T09:00:00Z", "latestEnd", "2026-09-07T15:00:00Z"),
                TimeWindowParams.class);
        assertEquals(Instant.parse("2026-09-07T09:00:00Z"), tw.earliestStart());
        assertEquals(Instant.parse("2026-09-07T15:00:00Z"), tw.latestEnd());
        assertNull(tw.latestStart());

        ForbiddenPairParams fp = ParamsBinder.bind(map("kind", "TASK_TASK", "relation", "SAME_RESOURCE",
                "pairs", List.of(map("a", "t1", "b", "t2"))), ForbiddenPairParams.class);
        assertEquals(ForbiddenPairParams.Kind.TASK_TASK, fp.kind());
        assertEquals("t2", fp.pairs().get(0).b());

        ResourceCapacityParams rc = ParamsBinder.bind(map("capacity", 0, "window",
                map("start", "2026-09-07T12:00:00Z", "end", "2026-09-07T14:00:00Z")), ResourceCapacityParams.class);
        assertEquals(0, rc.capacity());
        assertEquals(Instant.parse("2026-09-07T14:00:00Z"), rc.window().end());

        FixedAssignmentParams fa = ParamsBinder.bind(map("resourceIds", List.of("press-1")),
                FixedAssignmentParams.class);
        assertEquals(List.of("press-1"), fa.resourceIds());
        assertNull(fa.start());
    }

    @Test
    void emptyOrNullMapGivesTheRecordDefaults() {
        TaskEligibilityParams te = ParamsBinder.bind(null, TaskEligibilityParams.class);
        assertTrue(te.resourceIds().isEmpty());
        assertTrue(te.slotIds().isEmpty());
        assertEquals(LoadMetric.TICKS, ParamsBinder.bind(Map.of(), LoadObjectiveParams.class).metric());
        assertEquals(TardinessParams.WeightBy.NONE, ParamsBinder.bind(Map.of(), TardinessParams.class).weightBy());
        PlanChangeParams pc = ParamsBinder.bind(Map.of(), PlanChangeParams.class);
        assertEquals(1, pc.startDeviationWeight());
        assertEquals(1, pc.resourceChangeWeight());
        MaxLoadPerPeriodParams ml = ParamsBinder.bind(map("period", map("unit", "DAY"), "max", 8),
                MaxLoadPerPeriodParams.class);
        assertEquals(LoadMetric.TICKS, ml.metric());
        assertNull(ml.min());
    }

    @Test
    void voidParamsAcceptOnlyAnEmptyMap() {
        assertNull(ParamsBinder.bind(Map.of(), Void.class));
        assertNull(ParamsBinder.bind(null, Void.class));
        ParamsInvalidException e = assertThrows(ParamsInvalidException.class,
                () -> ParamsBinder.bind(map("x", 1), Void.class, "objectives[0].params"));
        assertEquals("PARAMS_INVALID", e.code());
        assertEquals("objectives[0].params.x", e.path());
    }

    @Test
    void unknownPropertyFailsWithItsPath() {
        ParamsInvalidException e = assertThrows(ParamsInvalidException.class,
                () -> ParamsBinder.bind(map("minRestTicks", 11, "minrest", 3), MinRestParams.class));
        assertEquals("minrest", e.path());
        assertTrue(e.getMessage().contains("minrest"));

        ParamsInvalidException prefixed = assertThrows(ParamsInvalidException.class,
                () -> ParamsBinder.bind(map("minRestTicks", 11, "minrest", 3), MinRestParams.class,
                        "constraints[2].params"));
        assertEquals("constraints[2].params.minrest", prefixed.path());
        assertEquals("PARAMS_INVALID", prefixed.code());
    }

    @Test
    void unknownNestedPropertyFails() {
        ParamsInvalidException e = assertThrows(ParamsInvalidException.class, () -> ParamsBinder.bind(
                map("period", map("unit", "DAY", "tick", 3), "max", 2), MaxConsecutiveParams.class));
        assertTrue(e.path().endsWith("tick"), e.path());
    }

    @Test
    void missingPrimitiveFails() {
        ParamsInvalidException e = assertThrows(ParamsInvalidException.class,
                () -> ParamsBinder.bind(map("period", map("unit", "DAY")), MaxConsecutiveParams.class));
        assertEquals("max", e.path());
        ParamsInvalidException prefixed = assertThrows(ParamsInvalidException.class,
                () -> ParamsBinder.bind(Map.of(), MinRestParams.class, "constraints[0].params"));
        assertEquals("constraints[0].params.minRestTicks", prefixed.path());
    }

    @Test
    void explicitNullForPrimitiveFails() {
        ParamsInvalidException e = assertThrows(ParamsInvalidException.class,
                () -> ParamsBinder.bind(map("minRestTicks", null), MinRestParams.class));
        assertTrue(e.path().endsWith("minRestTicks"), e.path());
    }

    @Test
    void noScalarCoercion() {
        assertThrows(ParamsInvalidException.class,
                () -> ParamsBinder.bind(map("minRestTicks", "11"), MinRestParams.class));
        assertThrows(ParamsInvalidException.class,
                () -> ParamsBinder.bind(map("minRestTicks", 11.5), MinRestParams.class));
        assertThrows(ParamsInvalidException.class,
                () -> ParamsBinder.bind(map("metric", 1), LoadObjectiveParams.class));
    }

    @Test
    void badEnumValueFailsWithPath() {
        ParamsInvalidException e = assertThrows(ParamsInvalidException.class, () -> ParamsBinder.bind(
                map("period", map("unit", "HOUR"), "max", 2), MaxConsecutiveParams.class,
                "constraints[1].params"));
        assertTrue(e.path().startsWith("constraints[1].params"), e.path());
        assertTrue(e.path().endsWith("unit"), e.path());
    }

    @Test
    void wrongShapeFails() {
        assertThrows(ParamsInvalidException.class,
                () -> ParamsBinder.bind(map("resourceIds", "press-1"), TaskEligibilityParams.class));
        assertThrows(ParamsInvalidException.class,
                () -> ParamsBinder.bind(map("period", "DAY", "max", 2), MaxConsecutiveParams.class));
        assertThrows(ParamsInvalidException.class,
                () -> ParamsBinder.bind(map("earliestStart", "not-a-date"), TimeWindowParams.class));
    }
}
