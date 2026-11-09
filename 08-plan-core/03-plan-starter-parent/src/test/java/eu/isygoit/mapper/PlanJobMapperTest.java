package eu.isygoit.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.dto.data.PlanJobDto;
import eu.isygoit.dto.data.PlanJobMinDto;
import eu.isygoit.dto.request.PlanningMeta;
import eu.isygoit.dto.request.PlanningRequest;
import eu.isygoit.dto.response.Diagnostics;
import eu.isygoit.enums.IEnumJobStatusCode;
import eu.isygoit.model.PlanJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link PlanJobMapper} / {@link PlanJobMinMapper} / {@link PlanJobJsonConverter}: json <-> typed payload conversion,
 * null safety and server-managed fields. The generated MapStruct implementations are instantiated directly (no Spring
 * context): the converter is injected by type.
 */
class PlanJobMapperTest {

    private PlanJobJsonConverter converter;
    private PlanJobMapper mapper;
    private PlanJobMinMapper minMapper;

    @BeforeEach
    void setUp() throws Exception {
        converter = new PlanJobJsonConverter(new ObjectMapper().findAndRegisterModules());
        mapper = new PlanJobMapperImpl();
        inject(mapper, converter);
        minMapper = new PlanJobMinMapperImpl();
    }

    private static void inject(Object target, Object dependency) throws Exception {
        for (Field field : target.getClass().getDeclaredFields()) {
            if (field.getType().isAssignableFrom(dependency.getClass())) {
                field.setAccessible(true);
                field.set(target, dependency);
                return;
            }
        }
        throw new IllegalStateException("No field of type " + dependency.getClass() + " in " + target.getClass());
    }

    private static PlanningRequest request() {
        return new PlanningRequest("acme", new PlanningMeta("ref-1", "1", "school", "en", "UTC"),
                null, null, null, null, null, null, null, null, null, null, null);
    }

    private PlanJob job() throws Exception {
        return PlanJob.builder()
                .id(7L).code("PLN-000007").tenant("acme").clientRef("ref-1").engine("cpsat")
                .status(IEnumJobStatusCode.Types.SUCCEEDED)
                .submittedAt(Instant.parse("2026-01-01T10:00:00Z"))
                .startedAt(Instant.parse("2026-01-01T10:00:05Z"))
                .completedAt(Instant.parse("2026-01-01T10:00:30Z"))
                .errorCode("E1").message("done")
                .requestJson(converter.requestToJson(request()))
                .diagnosticsJson(converter.diagnosticsToJson(new Diagnostics("why", List.of("c1"), null, List.of(), Map.of())))
                .build();
    }

    @Test
    void entityToDto_convertsJsonColumnsToTypedPayloads() throws Exception {
        PlanJobDto dto = mapper.entityToDto(job());

        assertEquals(7L, dto.getId());
        assertEquals("PLN-000007", dto.getCode());
        assertEquals("acme", dto.getTenant());
        assertEquals("cpsat", dto.getEngine());
        assertEquals(IEnumJobStatusCode.Types.SUCCEEDED, dto.getStatus());
        assertEquals("done", dto.getMessage());
        assertEquals(request(), dto.getRequest());
        assertEquals("why", dto.getDiagnostics().message());
        assertEquals(List.of("c1"), dto.getDiagnostics().infeasibilityCore());
        assertNull(dto.getResult(), "no resultJson -> no result");
    }

    @Test
    void entityToDto_isNullSafe() {
        assertNull(mapper.entityToDto(null));
        PlanJobDto dto = mapper.entityToDto(PlanJob.builder().id(1L).code("C").build());
        assertNull(dto.getRequest());
        assertNull(dto.getResult());
        assertNull(dto.getDiagnostics());
    }

    @Test
    void entityToDto_blankJsonIsTreatedAsAbsent() {
        PlanJobDto dto = mapper.entityToDto(PlanJob.builder().id(1L).requestJson("  ").resultJson("").build());
        assertNull(dto.getRequest());
        assertNull(dto.getResult());
    }

    @Test
    void entityToDto_unreadableStoredJsonIsAServerError() {
        PlanJob broken = PlanJob.builder().id(1L).requestJson("{not json").build();
        assertThrows(IllegalStateException.class, () -> mapper.entityToDto(broken));
    }

    @Test
    void converter_roundTripsAndIsNullSafe() {
        String json = converter.requestToJson(request());
        assertEquals(request(), converter.requestFromJson(json));
        assertNull(converter.requestToJson(null));
        assertNull(converter.requestFromJson(null));
        assertNull(converter.resultToJson(null));
        assertNull(converter.resultFromJson(null));
        assertNull(converter.diagnosticsToJson(null));
        assertNull(converter.diagnosticsFromJson(null));
    }

    @Test
    void dtoToEntity_takesOnlyTheIdAndIgnoresServerManagedFields() {
        PlanJobDto dto = PlanJobDto.builder()
                .id(9L).code("HACK").tenant("other-tenant").clientRef("x").engine("e")
                .status(IEnumJobStatusCode.Types.CANCELLED)
                .submittedAt(Instant.now()).errorCode("X").message("m")
                .request(request())
                .build();

        PlanJob entity = mapper.dtoToEntity(dto);

        assertEquals(9L, entity.getId());
        assertNull(entity.getCode());
        assertNull(entity.getTenant());
        assertNull(entity.getClientRef());
        assertNull(entity.getEngine());
        assertNull(entity.getStatus());
        assertNull(entity.getSubmittedAt());
        assertNull(entity.getErrorCode());
        assertNull(entity.getMessage());
        assertNull(entity.getRequestJson());
        assertNull(entity.getResultJson());
        assertNull(entity.getDiagnosticsJson());
        assertNull(mapper.dtoToEntity(null));
    }

    @Test
    void minMapper_neverTouchesTheJsonColumns() throws Exception {
        PlanJob withGarbage = job();
        withGarbage.setRequestJson("{not json");
        withGarbage.setResultJson("also not json");

        PlanJobMinDto dto = minMapper.entityToDto(withGarbage); // would throw if the json columns were read

        assertEquals(7L, dto.getId());
        assertEquals("PLN-000007", dto.getCode());
        assertEquals(IEnumJobStatusCode.Types.SUCCEEDED, dto.getStatus());
        assertEquals(PlanJobMinDto.class, dto.getClass(), "min mapper produces the lightweight dto");

        PlanJob back = minMapper.dtoToEntity(dto);
        assertEquals(7L, back.getId());
        assertNull(back.getRequestJson());
        assertNull(back.getResultJson());
        assertNull(back.getDiagnosticsJson());
        assertNull(back.getTenant());
        assertNull(minMapper.entityToDto(null));
    }
}
