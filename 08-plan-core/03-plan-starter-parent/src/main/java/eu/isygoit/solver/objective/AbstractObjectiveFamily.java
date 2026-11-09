package eu.isygoit.solver.objective;

import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.Objective;
import eu.isygoit.solver.spi.CheckContext;
import eu.isygoit.solver.spi.ObjectiveEvaluator;
import eu.isygoit.solver.spi.ObjectiveFamily;
import eu.isygoit.solver.spi.ScopeKind;
import eu.isygoit.solver.spi.ValidationContext;

import java.util.List;
import java.util.Set;

/**
 * Shared skeleton of the 13 catalogue objectives (design 4.4): identity, i18n key, scope-kind and weight validation, and
 * the evaluator wiring. A concrete family only adds its params checks, its cost estimate and its exact term
 * ({@link #evaluate}).
 * <p>
 * Stateless and thread-safe. Scopes: TASKS families accept {@code all|task|taskType|tag}; RESOURCES families accept
 * {@code all|resource|resourceId|resourceType|tag} (tag = resource tag).
 */
public abstract class AbstractObjectiveFamily implements ObjectiveFamily {

    /** Error code of a scope kind the family cannot use (existing catalogue code, design 7.4). */
    public static final String INVALID_SCOPE = "INVALID_SCOPE";
    /** Error code of a scope or preference referencing an unknown id (existing catalogue code). */
    public static final String UNKNOWN_REFERENCE = "UNKNOWN_REFERENCE";
    /** Error code of an invalid family parameter value. */
    public static final String OBJECTIVE_PARAMS_INVALID = "OBJECTIVE_PARAMS_INVALID";
    /** Warning code: the weight is 0, the objective is disabled. */
    public static final String OBJECTIVE_WEIGHT_ZERO = "OBJECTIVE_WEIGHT_ZERO";
    /** Warning code: the objective cannot change the result on this problem. */
    public static final String OBJECTIVE_NO_EFFECT = "OBJECTIVE_NO_EFFECT";

    private static final Set<String> TASK_KINDS = Set.of("all", "task", "taskType", "tag");
    private static final Set<String> RESOURCE_KINDS = Set.of("all", "resource", "resourceId", "resourceType", "tag");

    private final String type;
    private final Class<?> paramsType;
    private final boolean reward;
    private final List<String> paramsHint;
    private final ScopeKind scopeKind;

    /**
     * @param type       family id (catalogue 4.4)
     * @param paramsType typed params record or {@code Void.class}
     * @param reward     {@code true} for a maximisation term
     * @param paramsHint params property names for the UI
     * @param scopeKind  {@link ScopeKind#TASKS} or {@link ScopeKind#RESOURCES}
     */
    protected AbstractObjectiveFamily(String type, Class<?> paramsType, boolean reward, List<String> paramsHint,
                                      ScopeKind scopeKind) {
        this.type = type;
        this.paramsType = paramsType;
        this.reward = reward;
        this.paramsHint = List.copyOf(paramsHint);
        this.scopeKind = scopeKind;
    }

    @Override
    public final String type() {
        return type;
    }

    @Override
    public final Class<?> paramsType() {
        return paramsType;
    }

    @Override
    public final String descriptionKey() {
        return "plan.objective." + type + ".description";
    }

    @Override
    public final List<String> paramsHint() {
        return paramsHint;
    }

    @Override
    public final boolean reward() {
        return reward;
    }

    @Override
    public final void validate(ValidationContext ctx, Objective objective, Object params) {
        if (objective.weight() == 0) {
            ctx.warning(OBJECTIVE_WEIGHT_ZERO, path(ctx, objective, "weight"),
                    "objective '" + objective.id() + "' has weight 0 and is disabled");
        }
        validateScope(ctx, objective);
        validateParams(ctx, objective, params);
    }

    /** Family specific checks (params values, data that makes the objective meaningless). Default: none. */
    protected void validateParams(ValidationContext ctx, Objective objective, Object params) {
    }

    /**
     * Exact value of the term before the weight.
     *
     * @see ObjectiveEvaluator#evaluate(CheckContext, Objective, Object)
     */
    protected abstract long evaluate(CheckContext ctx, Objective objective, Object params);

    @Override
    public final ObjectiveEvaluator evaluator() {
        return this::evaluate;
    }

    /** @return the JSON path of a field of the objective, {@code objectives[i].<suffix>} (index found by identity or id) */
    protected static String path(ValidationContext ctx, Objective objective, String suffix) {
        List<Objective> all = ctx.problem().objectives();
        int idx = -1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i) == objective || (objective.id() != null && objective.id().equals(all.get(i).id()))) {
                idx = i;
                break;
            }
        }
        return "objectives[" + Math.max(idx, 0) + "]." + suffix;
    }

    /** Emits the {@code OBJECTIVE_NO_EFFECT} warning. */
    protected static void noEffect(ValidationContext ctx, Objective objective, String reason) {
        ctx.warning(OBJECTIVE_NO_EFFECT, path(ctx, objective, "type"),
                "objective '" + objective.id() + "' has no effect: " + reason);
    }

    private void validateScope(ValidationContext ctx, Objective objective) {
        ConstraintScope scope = objective.scope();
        String kind = scope.kind();
        Set<String> allowed = scopeKind == ScopeKind.RESOURCES ? RESOURCE_KINDS : TASK_KINDS;
        String path = path(ctx, objective, "scope");
        if (!allowed.contains(kind)) {
            ctx.error(INVALID_SCOPE, path + ".kind",
                    "scope kind '" + kind + "' is not valid for '" + type + "' (allowed: " + allowed + ")");
            return;
        }
        if (!"all".equals(kind) && (scope.value() == null || scope.value().isBlank())) {
            ctx.error(INVALID_SCOPE, path + ".value", "scope kind '" + kind + "' needs a value");
            return;
        }
        if ("task".equals(kind) && !ctx.hasTask(scope.value())) {
            ctx.error(UNKNOWN_REFERENCE, path + ".value", "unknown task '" + scope.value() + "'");
        }
        if (("resource".equals(kind) || "resourceId".equals(kind)) && !ctx.hasResource(scope.value())) {
            ctx.error(UNKNOWN_REFERENCE, path + ".value", "unknown resource '" + scope.value() + "'");
        }
    }
}
