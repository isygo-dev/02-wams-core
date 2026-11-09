package eu.isygoit.dto.request;

import eu.isygoit.util.ImmutableJsonMap;

import java.util.Map;

/**
 * An objective term (family id in {@code type}). All objectives are minimisation terms internally; maximisation
 * families are negated by the engine and reported sign-normalised.
 * <pre>{@code
 * { "id": "fair", "type": "minimize_max_load", "weight": 1, "params": { "metric": "TICKS" } }
 * }</pre>
 * Defaults applied here: {@code tier} = 0, {@code scope} = {@code {kind:"all"}}, {@code params} = empty.
 * {@code scale} stays null when omitted (the normalizer applies {@code solverOptions.objectiveScale}). A negative
 * weight is rejected; the range of tier/scale is checked by the verifier.
 *
 * @param id     unique objective id
 * @param type   objective family id
 * @param weight {@code >= 0} multiplier of the term inside its tier; 0 = disabled (warning)
 * @param tier   lexicographic tier 0..7, default 0 (lower = more important); -1 and -2 are reserved
 * @param scale  decimal to long multiplier for THIS objective; null = {@code solverOptions.objectiveScale}
 * @param scope  optional subset of tasks/resources the objective looks at, default all
 * @param params bound to the family's typed record (usually empty)
 */
public record Objective(
        String id,
        String type,
        int weight,
        Integer tier,
        Integer scale,
        ConstraintScope scope,
        Map<String, Object> params) {

    public Objective {
        if (weight < 0) {
            throw new IllegalArgumentException("Objective weight must not be negative");
        }
        tier = tier == null ? Integer.valueOf(0) : tier;
        scope = scope == null ? ConstraintScope.all() : scope;
        params = ImmutableJsonMap.copyOf(params);
    }

    /**
     * Convenience constructor: tier 0, default scale, scope all, no params.
     */
    public Objective(String id, String type, int weight) {
        this(id, type, weight, null, null, null, Map.of());
    }
}
