package eu.isygoit.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.dto.request.PlanningRequest;
import eu.isygoit.dto.response.Diagnostics;
import eu.isygoit.dto.response.SolveResult;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

/**
 * JSON conversion helper of the {@link PlanJobMapper}: the {@link eu.isygoit.model.PlanJob} entity stores the request,
 * the result and the diagnostics as jsonb strings while {@link eu.isygoit.dto.data.PlanJobDto} carries them typed.
 * <p>
 * Uses the Spring-managed {@link ObjectMapper} (same configuration as the REST layer and the queue). All methods are
 * null-safe: {@code null} or blank in, {@code null} out. A stored payload that cannot be (de)serialized is a server
 * side inconsistency and raises an {@link IllegalStateException} (never an {@link IllegalArgumentException}, which the
 * exception handler would report as a client error).
 */
@Component
public class PlanJobJsonConverter {

    private final ObjectMapper objectMapper;

    /**
     * Instantiates a new Plan job json converter.
     *
     * @param objectMapper the Spring-managed object mapper
     */
    public PlanJobJsonConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Request json to typed request.
     *
     * @param json the stored json
     * @return the request or null
     */
    @Named("requestFromJson")
    public PlanningRequest requestFromJson(String json) {
        return read(json, PlanningRequest.class);
    }

    /**
     * Typed request to json.
     *
     * @param request the request
     * @return the json or null
     */
    @Named("requestToJson")
    public String requestToJson(PlanningRequest request) {
        return write(request);
    }

    /**
     * Result json to typed result.
     *
     * @param json the stored json
     * @return the result or null
     */
    @Named("resultFromJson")
    public SolveResult resultFromJson(String json) {
        return read(json, SolveResult.class);
    }

    /**
     * Typed result to json.
     *
     * @param result the result
     * @return the json or null
     */
    @Named("resultToJson")
    public String resultToJson(SolveResult result) {
        return write(result);
    }

    /**
     * Diagnostics json to typed diagnostics.
     *
     * @param json the stored json
     * @return the diagnostics or null
     */
    @Named("diagnosticsFromJson")
    public Diagnostics diagnosticsFromJson(String json) {
        return read(json, Diagnostics.class);
    }

    /**
     * Typed diagnostics to json.
     *
     * @param diagnostics the diagnostics
     * @return the json or null
     */
    @Named("diagnosticsToJson")
    public String diagnosticsToJson(Diagnostics diagnostics) {
        return write(diagnostics);
    }

    private <T> T read(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored " + type.getSimpleName() + " payload is not readable", e);
        }
    }

    private String write(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize " + value.getClass().getSimpleName(), e);
        }
    }
}
