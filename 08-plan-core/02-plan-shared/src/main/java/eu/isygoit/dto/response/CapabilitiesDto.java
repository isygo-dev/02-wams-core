package eu.isygoit.dto.response;

import eu.isygoit.dto.request.PlanningMeta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What the plan service can solve, per engine: available solvers with their supported features, the known
 * constraint and objective families with per-engine support, the schema version and the effective limits.
 * <p>
 * Example fragment of a constraint entry:
 * {@code {"type":"min_rest","engines":{"cp_sat":{"hardCapable":true,"softCapable":true,"relaxable":true},
 * "timefold":{"hardCapable":false,"softCapable":false,"relaxable":false}}}}.
 * The flat {@code hardCapable/softCapable/relaxable} fields of a constraint are the UNION view over all engines
 * for simple clients.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapabilitiesDto {

    /**
     * Available solvers with their per-engine details.
     */
    @Builder.Default
    private List<SolverCapability> solvers = new ArrayList<>();

    /**
     * Union of the known constraint types.
     */
    @Builder.Default
    private List<ConstraintCapability> constraints = new ArrayList<>();

    /**
     * Union of the known objective types.
     */
    @Builder.Default
    private List<ObjectiveCapability> objectives = new ArrayList<>();

    /**
     * Request schema version accepted by the service. Example: {@code "2.0"}.
     */
    @Builder.Default
    private String schemaVersion = PlanningMeta.SCHEMA_VERSION;

    /**
     * Effective {@code app.plan.limits} so a UI can pre-check sizes.
     */
    private LimitsView limits;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SolverCapability {
        /**
         * Solver id as accepted in the solver options. Example: {@code "cp_sat"}.
         */
        private String id;
        private String label;
        /**
         * {@code false} when the solver is known but not usable on this instance.
         */
        private boolean available;
        /**
         * {@code true} when the engine can prove optimality / infeasibility (Timefold cannot).
         */
        private boolean provesOptimality;
        /**
         * Supported {@code EngineCapabilities.Feature} names.
         */
        @Builder.Default
        private List<String> features = new ArrayList<>();
        /**
         * Features the engine does not support (everything else).
         */
        @Builder.Default
        private List<String> unsupportedFeatures = new ArrayList<>();
        @Builder.Default
        private List<String> unsupportedConstraintTypes = new ArrayList<>();
        @Builder.Default
        private List<String> unsupportedObjectiveTypes = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConstraintCapability {
        /**
         * Constraint type as used in {@code Constraint.type}.
         */
        private String type;
        private String description;
        /**
         * Names (and short meaning) of the expected params. Example: {@code ["minRestTicks","afterTasks"]}.
         */
        @Builder.Default
        private List<String> paramsHint = new ArrayList<>();
        /**
         * Kind of scope the family expects: TASKS | RESOURCES | GROUPS | NONE.
         */
        private String scopeKind;
        /**
         * Per-engine support, keyed by engine id.
         */
        @Builder.Default
        private Map<String, EngineSupport> engines = new HashMap<>();
        /**
         * UNION view: can be declared HARD on at least one engine.
         */
        private boolean hardCapable;
        /**
         * UNION view: can be declared SOFT (penalised) on at least one engine.
         */
        private boolean softCapable;
        /**
         * UNION view: supports a relaxation policy on at least one engine.
         */
        private boolean relaxable;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EngineSupport {
        private boolean hardCapable;
        private boolean softCapable;
        private boolean relaxable;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ObjectiveCapability {
        /**
         * Objective type as used in {@code Objective.type}.
         */
        private String type;
        private String description;
        /**
         * Names (and short meaning) of the objective parameters.
         */
        @Builder.Default
        private List<String> params = new ArrayList<>();
        /**
         * {@code true} for a maximisation term (reported sign-normalised).
         */
        private boolean reward;
        /**
         * Ids of the engines supporting it.
         */
        @Builder.Default
        private List<String> engines = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LimitsView {
        private int maxTasks;
        private int maxResources;
        private int maxSlots;
        private int maxConstraints;
        private int maxHorizonTicks;
        private int maxTimeSeconds;
        private int maxWorkers;
        private long maxCandidates;
        private long maxRequestBytes;
    }
}
