package eu.isygoit.dto.response;

import eu.isygoit.dto.request.PlanningRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of a dry-run validation of a {@link PlanningRequest} (nothing is persisted or solved). Carries the
 * compatibility of the request with EVERY engine, so a UI can grey out options per engine.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationReportDto {

    /**
     * {@code true} when the request has no error for the REQUESTED engine.
     */
    private boolean valid;

    /**
     * The request after normalization (defaults applied); {@code null} when stage 1 failed.
     */
    private PlanningRequest normalizedRequest;

    /**
     * Validation errors (empty when valid).
     */
    @Builder.Default
    private List<Issue> errors = new ArrayList<>();

    /**
     * Validation warnings (never make the request invalid).
     */
    @Builder.Default
    private List<Issue> warnings = new ArrayList<>();

    /**
     * Engine id to "the request is accepted by that engine" (capability stage).
     */
    @Builder.Default
    private Map<String, Boolean> engineCompatible = new HashMap<>();

    /**
     * Engine id to the capability issues of that engine.
     */
    @Builder.Default
    private Map<String, List<Issue>> engineIssues = new HashMap<>();

    /**
     * Estimated model size (null when it could not be computed).
     */
    private ModelEstimateDto estimate;

    /**
     * One validation issue.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Issue {
        /**
         * Stable code. Example: {@code "UNKNOWN_REFERENCE"}.
         */
        private String code;
        /**
         * ERROR | WARNING.
         */
        private String severity;
        /**
         * Human readable message.
         */
        private String message;
        /**
         * Location in the request. Example: {@code "tasks[3].predecessors[0].taskId"}.
         */
        private String path;
        /**
         * Values for the i18n placeholders of the message key {@code plan.error.<code>}.
         */
        @Builder.Default
        private Map<String, Object> args = new HashMap<>();
    }
}
