package eu.isygoit.dto.request;

import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.util.ImmutableJsonMap;

import java.util.Map;

/**
 * A catalogue constraint instance (family id in {@code type}, parameters in {@code params}).
 * <pre>{@code
 * { "id": "rest-11h", "type": "min_rest", "enforcement": "STRICTLY_REQUIRED",
 *   "scope": { "kind": "all" }, "params": { "minRestTicks": 11 } }
 * }</pre>
 * Enforcement semantics: STRICTLY_REQUIRED and CONFIGURABLE_HARD are HARD (CONFIGURABLE_HARD only marks "the UI may
 * turn it into SOFT"); RELAXABLE_WITH_AUTHORIZATION is HARD unless {@code relaxation.authorized} and the solver
 * cannot satisfy it; SOFT is penalised. STRICTLY_REQUIRED is never relaxed or softened.
 * <p>
 * Defaults applied here: {@code enforcement} = STRICTLY_REQUIRED, {@code scope} = {@code {kind:"all"}},
 * {@code params} = empty, {@code tier} = 0, and {@code weight} = 1 for SOFT constraints when 0 is given. A negative
 * weight is rejected. The params map is bound to the typed record of the family by
 * {@link eu.isygoit.util.ParamsBinder}.
 *
 * @param id          unique constraint id
 * @param type        family id, see the catalogue (for example "min_rest")
 * @param enforcement HARD/SOFT/RELAXABLE semantics, default STRICTLY_REQUIRED
 * @param scope       default {@code {kind:"all"}}
 * @param params      bound to the family's typed record
 * @param relaxation  only read for RELAXABLE_WITH_AUTHORIZATION
 * @param weight      SOFT: penalty per violation unit ({@code >= 0}); default 1 for SOFT when 0 is given
 * @param tier        SOFT: lexicographic tier, default 0
 */
public record Constraint(
        String id,
        String type,
        IEnumEnforcement.Types enforcement,
        ConstraintScope scope,
        Map<String, Object> params,
        RelaxationPolicy relaxation,
        int weight,
        Integer tier) {

    public Constraint {
        enforcement = enforcement == null ? IEnumEnforcement.Types.STRICTLY_REQUIRED : enforcement;
        scope = scope == null ? ConstraintScope.all() : scope;
        params = ImmutableJsonMap.copyOf(params);
        if (weight < 0) {
            throw new IllegalArgumentException("Constraint weight must not be negative");
        }
        if (weight == 0 && enforcement == IEnumEnforcement.Types.SOFT) {
            weight = 1;
        }
        tier = tier == null ? Integer.valueOf(0) : tier;
    }
}
