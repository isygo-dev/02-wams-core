package eu.isygoit.service.impl.planning;

import eu.isygoit.config.PlanLimitsProperties;
import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.Slot;
import eu.isygoit.dto.request.SolverOptions;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.exception.ParamsInvalidException;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.CompiledObjective;
import eu.isygoit.solver.EngineCapabilities;
import eu.isygoit.solver.EngineCapabilities.Feature;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.index.LightIndexView;
import eu.isygoit.solver.objective.AutoObjectives;
import eu.isygoit.solver.registry.ConstraintFamilyRegistry;
import eu.isygoit.solver.registry.ObjectiveFamilyRegistry;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.ConstraintFamily;
import eu.isygoit.solver.spi.ObjectiveFamily;
import eu.isygoit.solver.spi.ScopeKind;
import eu.isygoit.solver.spi.TaskRewriter;
import eu.isygoit.solver.spi.ValidationContext;
import eu.isygoit.util.ParamsBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Stage 3 of the validation pipeline (design 7.2): semantic validation by the constraint and objective families and the
 * per-engine capability check, both on the NORMALIZED problem.
 * <p>
 * {@link #verifySemantics} binds the typed params ({@code PARAMS_INVALID}), checks enforcement
 * ({@code INVALID_ENFORCEMENT}), scope kind ({@code INVALID_SCOPE}), SOFT weight and tier ({@code INVALID_WEIGHT},
 * {@code INVALID_TIER}), relaxation policy, then calls {@code family.validate} with a {@link ValidationContext} backed by
 * the light index view. It returns the compiled constraints and objectives (a constraint fully handled by a
 * {@link TaskRewriter} is {@code active = false}). {@link #checkCapabilities} computes the features the problem
 * requires ({@link FeatureScanner}) and reports ONE issue per missing feature, per unsupported constraint type,
 * enforcement or objective, plus {@code OVERLAPPING_SLOTS} for the Timefold engine; it never mutates anything, so the same
 * problem can be checked against every engine. Everything is collected into a {@link ValidationResult}.
 * <p>
 * Stateless apart from the (immutable) registries and limits; thread-safe.
 */
@Component
public class ProblemVerifier {

    /** Highest user tier (design 4.4: tiers 0..7, -1 and -2 are reserved). */
    public static final int MAX_TIER = 7;
    /** Highest relaxation priority accepted. */
    public static final int MAX_RELAXATION_PRIORITY = 1000;
    private static final int MAX_OBJECTIVE_SCALE = SolverOptions.MAX_OBJECTIVE_SCALE;

    private static final Set<String> TASK_KINDS = Set.of(ConstraintScope.ALL, "task", "taskType", "tag");
    private static final Set<String> RESOURCE_KINDS =
            Set.of(ConstraintScope.ALL, "resource", "resourceId", "resourceType", "tag");
    private static final Set<String> GROUP_KINDS = Set.of(ConstraintScope.ALL, "tag");
    private static final Set<String> OBJECTIVE_KINDS =
            Set.of(ConstraintScope.ALL, "task", "taskType", "resource", "resourceId", "resourceType", "tag");

    /**
     * Result of {@link #verifySemantics}.
     *
     * @param constraints compiled constraints in request order (unknown or unbindable ones are absent)
     * @param objectives  compiled objectives in request order
     */
    public record Verified(List<CompiledConstraint> constraints, List<CompiledObjective> objectives) {
    }

    private final ConstraintFamilyRegistry constraintRegistry;
    private final ObjectiveFamilyRegistry objectiveRegistry;
    private final PlanLimitsProperties limits;

    /**
     * Creates the verifier.
     *
     * @param constraintRegistry constraint families
     * @param objectiveRegistry  objective families
     * @param limits             configured limits
     */
    @Autowired
    public ProblemVerifier(ConstraintFamilyRegistry constraintRegistry, ObjectiveFamilyRegistry objectiveRegistry,
                           PlanLimitsProperties limits) {
        this.constraintRegistry = constraintRegistry;
        this.objectiveRegistry = objectiveRegistry;
        this.limits = limits;
    }

    // ==================================================================================================
    // Semantic validation
    // ==================================================================================================

    /**
     * Verifies constraints and objectives of a normalized problem.
     *
     * @param problem the normalized problem
     * @param out     receives every issue
     * @return the compiled constraints and objectives
     */
    public Verified verifySemantics(PlanningProblem problem, ValidationResult out) {
        CandidateIndexView index = new LightIndexView(problem, limits);
        Context ctx = new Context(problem, index, out);
        List<CompiledConstraint> compiledConstraints = new ArrayList<>();
        for (int i = 0; i < problem.constraints().size(); i++) {
            CompiledConstraint compiled = verifyConstraint(ctx, i, problem.constraints().get(i), out);
            if (compiled != null) {
                compiledConstraints.add(compiled);
            }
        }
        List<CompiledObjective> compiledObjectives = new ArrayList<>();
        for (int i = 0; i < problem.objectives().size(); i++) {
            CompiledObjective compiled = verifyObjective(ctx, problem, i, problem.objectives().get(i), out);
            if (compiled != null) {
                compiledObjectives.add(compiled);
            }
        }
        return new Verified(compiledConstraints, compiledObjectives);
    }

    private CompiledConstraint verifyConstraint(Context ctx, int i, Constraint c, ValidationResult out) {
        String at = "constraints[" + i + "]";
        ConstraintFamily family = constraintRegistry.find(c.type()).orElse(null);
        if (family == null) {
            out.error("UNKNOWN_CONSTRAINT", at + ".type", "Unknown constraint type '" + c.type()
                    + "'. Supported types: " + constraintRegistry.supportedTypes(),
                    args("type", c.type(), "supported", String.valueOf(constraintRegistry.supportedTypes())));
            return null;
        }
        boolean bound = true;
        Object params = null;
        try {
            params = ParamsBinder.bind(c.params(), family.paramsType(), at + ".params");
        } catch (ParamsInvalidException e) {
            out.error(ParamsInvalidException.CODE, e.path(), e.getMessage(), args("type", c.type()));
            bound = false;
        }
        if (!family.supportedEnforcements().contains(c.enforcement())) {
            out.error("INVALID_ENFORCEMENT", at + ".enforcement", "Constraint type '" + c.type()
                    + "' does not support enforcement " + c.enforcement() + " (supported: "
                    + family.supportedEnforcements() + ")", args("type", c.type(), "enforcement", c.enforcement().name()));
        }
        checkScope(out, at + ".scope", c.scope(), kindsOf(family.scopeKind()), c.type());
        if (c.enforcement() == IEnumEnforcement.Types.SOFT) {
            if (c.weight() < 1) {
                out.error("INVALID_WEIGHT", at + ".weight", "A SOFT constraint needs a weight >= 1",
                        args("weight", c.weight()));
            }
            int tier = c.tier() == null ? 0 : c.tier();
            if (tier < 0 || tier > MAX_TIER) {
                out.error("INVALID_TIER", at + ".tier", "tier must be between 0 and " + MAX_TIER
                        + " (negative tiers are reserved)", args("tier", tier));
            }
        }
        if (c.enforcement() == IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION) {
            if (c.relaxation() == null) {
                out.error("INVALID_ENFORCEMENT", at + ".relaxation",
                        "RELAXABLE_WITH_AUTHORIZATION requires a relaxation policy", args("type", c.type()));
            } else if (c.relaxation().priority() > MAX_RELAXATION_PRIORITY) {
                out.error("INVALID_ENFORCEMENT", at + ".relaxation.priority",
                        "relaxation priority must not exceed " + MAX_RELAXATION_PRIORITY,
                        args("priority", c.relaxation().priority()));
            }
        }
        if (!bound) {
            return null;
        }
        family.validate(ctx, c, params);
        boolean active = !(family instanceof TaskRewriter && DomainNormalizer.isHard(c.enforcement()));
        return new CompiledConstraint(i, c, family, params, active);
    }

    private CompiledObjective verifyObjective(Context ctx, PlanningProblem problem, int i, Objective o,
                                              ValidationResult out) {
        String at = "objectives[" + i + "]";
        ObjectiveFamily family = objectiveRegistry.find(o.type()).orElse(null);
        if (family == null) {
            out.error("UNKNOWN_OBJECTIVE", at + ".type", "Unknown objective type '" + o.type()
                    + "'. Supported types: " + objectiveRegistry.supportedTypes(),
                    args("type", o.type(), "supported", String.valueOf(objectiveRegistry.supportedTypes())));
            return null;
        }
        Object params = null;
        boolean bound = true;
        try {
            params = ParamsBinder.bind(o.params(), family.paramsType(), at + ".params");
        } catch (ParamsInvalidException e) {
            out.error(ParamsInvalidException.CODE, e.path(), e.getMessage(), args("type", o.type()));
            bound = false;
        }
        int tier = o.tier() == null ? 0 : o.tier();
        boolean builtIn = AutoObjectives.AUTO_ID.equals(o.id()) && tier == AutoObjectives.AUTO_TIER;
        if (!builtIn && (tier < 0 || tier > MAX_TIER)) {
            out.error("INVALID_TIER", at + ".tier", "tier must be between 0 and " + MAX_TIER
                    + " (negative tiers are reserved)", args("tier", tier));
        }
        int scale = o.scale() == null ? problem.options().objectiveScale() : o.scale();
        if (scale < 1 || scale > MAX_OBJECTIVE_SCALE) {
            out.error("INVALID_WEIGHT", at + ".scale", "scale must be between 1 and " + MAX_OBJECTIVE_SCALE,
                    args("scale", scale));
        }
        checkScope(out, at + ".scope", o.scope(), OBJECTIVE_KINDS, o.type());
        if (o.weight() == 0) {
            out.warning("OBJECTIVE_WEIGHT_ZERO", at + ".weight", "objective '" + o.id() + "' has weight 0 and is disabled",
                    args("id", o.id()));
        }
        if (!bound) {
            return null;
        }
        family.validate(ctx, o, params);
        return new CompiledObjective(i, o, family, params, tier, scale);
    }

    private static Set<String> kindsOf(ScopeKind kind) {
        switch (kind) {
            case TASKS:
                return TASK_KINDS;
            case RESOURCES:
                return RESOURCE_KINDS;
            case GROUPS:
                return GROUP_KINDS;
            default:
                return null; // NONE: the scope is ignored
        }
    }

    private static void checkScope(ValidationResult out, String path, ConstraintScope scope, Set<String> allowed,
                                   String type) {
        if (allowed == null || scope == null || allowed.contains(scope.kind())) {
            return;
        }
        out.error("INVALID_SCOPE", path + ".kind", "scope kind '" + scope.kind() + "' is not valid for '" + type
                + "' (allowed: " + allowed + ")", args("kind", scope.kind(), "type", type));
    }

    // ==================================================================================================
    // Capability check
    // ==================================================================================================

    /**
     * Checks the problem against one engine.
     *
     * @param problem the normalized problem
     * @param engine  the capabilities of the engine
     * @param out     receives one issue per miss (and warnings for informational features)
     */
    public void checkCapabilities(PlanningProblem problem, EngineCapabilities engine, ValidationResult out) {
        String engineId = engine.engineId();
        for (Map.Entry<Feature, String> need : FeatureScanner.scan(problem).entrySet()) {
            if (!engine.supports(need.getKey())) {
                out.error("ENGINE_UNSUPPORTED_FEATURE", need.getValue(), "Engine '" + engineId
                        + "' does not support the feature " + need.getKey() + " (first used here)",
                        args("feature", need.getKey().name(), "engine", engineId));
            }
        }
        if (Boolean.TRUE.equals(problem.options().explainInfeasibility())
                && !engine.supports(Feature.EXPLAIN_ASSUMPTIONS)) {
            out.warning("ENGINE_UNSUPPORTED_FEATURE", "solverOptions.explainInfeasibility",
                    "Engine '" + engineId + "' cannot explain infeasibility with assumptions; only the structural"
                            + " pre-solve analysis is available",
                    args("feature", Feature.EXPLAIN_ASSUMPTIONS.name(), "engine", engineId));
        }
        for (int i = 0; i < problem.constraints().size(); i++) {
            Constraint c = problem.constraints().get(i);
            if (isRewritten(c)) {
                continue;
            }
            String at = "constraints[" + i + "]";
            String type = c.type() == null ? "" : c.type().trim().toLowerCase(Locale.ROOT);
            EnforcementSupport support = engine.constraints().get(type);
            if (support == null) {
                out.error("ENGINE_UNSUPPORTED_CONSTRAINT", at, "Engine '" + engineId
                        + "' does not support the constraint type '" + c.type() + "'",
                        args("type", c.type(), "engine", engineId));
            } else if (!supports(support, c)) {
                out.error("ENGINE_UNSUPPORTED_ENFORCEMENT", at + ".enforcement", "Engine '" + engineId
                        + "' does not support enforcement " + c.enforcement() + " for the constraint type '"
                        + c.type() + "'", args("type", c.type(), "enforcement", c.enforcement().name(),
                        "engine", engineId));
            }
        }
        for (int i = 0; i < problem.objectives().size(); i++) {
            Objective o = problem.objectives().get(i);
            String type = o.type() == null ? "" : o.type().trim().toLowerCase(Locale.ROOT);
            if (!engine.objectives().contains(type)) {
                out.error("ENGINE_UNSUPPORTED_OBJECTIVE", "objectives[" + i + "]", "Engine '" + engineId
                        + "' does not support the objective type '" + o.type() + "'",
                        args("type", o.type(), "engine", engineId));
            }
        }
        if (SolverOptions.ENGINE_TIMEFOLD.equals(engineId)) {
            checkDisjointSlots(problem, out);
        }
    }

    private boolean isRewritten(Constraint c) {
        if (c.type() == null || !DomainNormalizer.isHard(c.enforcement())) {
            return false;
        }
        return constraintRegistry.find(c.type()).map(f -> f instanceof TaskRewriter).orElse(false);
    }

    private static boolean supports(EnforcementSupport support, Constraint c) {
        switch (c.enforcement()) {
            case SOFT:
                return support.soft();
            case RELAXABLE_WITH_AUTHORIZATION:
                return c.relaxation() != null && c.relaxation().authorized() ? support.relaxable() : support.hard();
            default:
                return support.hard();
        }
    }

    private static void checkDisjointSlots(PlanningProblem problem, ValidationResult out) {
        List<Integer> order = new ArrayList<>();
        List<Slot> slots = problem.slots();
        for (int s = 0; s < slots.size(); s++) {
            order.add(s);
        }
        order.sort(Comparator.<Integer, Instant>comparing(s -> slots.get(s).start()).thenComparing(s -> s));
        Instant maxEnd = null;
        int maxEndSlot = -1;
        for (int s : order) {
            Slot slot = slots.get(s);
            if (maxEnd != null && slot.start().isBefore(maxEnd)) {
                out.error("OVERLAPPING_SLOTS", "slots[" + s + "]", "Slot '" + slot.id() + "' overlaps slot '"
                        + slots.get(maxEndSlot).id() + "': this engine needs pairwise disjoint slots",
                        args("slot", slot.id(), "other", slots.get(maxEndSlot).id()));
                return;
            }
            if (maxEnd == null || slot.end().isAfter(maxEnd)) {
                maxEnd = slot.end();
                maxEndSlot = s;
            }
        }
    }

    private static Map<String, Object> args(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return map;
    }

    // ==================================================================================================
    // ValidationContext
    // ==================================================================================================

    /** Context handed to the families: light index view, strict params binding, issue sink. */
    private static final class Context implements ValidationContext {

        private final PlanningProblem problem;
        private final CandidateIndexView index;
        private final ValidationResult out;

        Context(PlanningProblem problem, CandidateIndexView index, ValidationResult out) {
            this.problem = problem;
            this.index = index;
            this.out = out;
        }

        @Override
        public PlanningProblem problem() {
            return problem;
        }

        @Override
        public CandidateIndexView index() {
            return index;
        }

        @Override
        public <P> P params(Constraint c, Class<P> type) {
            int at = problem.constraints().indexOf(c);
            try {
                return ParamsBinder.bind(c.params(), type, at < 0 ? "params" : "constraints[" + at + "].params");
            } catch (ParamsInvalidException e) {
                out.error(ParamsInvalidException.CODE, e.path(), e.getMessage(), args("type", c.type()));
                return null;
            }
        }

        @Override
        public void error(String code, String path, String message) {
            out.error(code, path, message);
        }

        @Override
        public void warning(String code, String path, String message) {
            out.warning(code, path, message);
        }

        @Override
        public boolean hasTask(String id) {
            return index.taskIdx(id) >= 0;
        }

        @Override
        public boolean hasResource(String id) {
            return index.resourceIdx(id) >= 0;
        }

        @Override
        public boolean hasSlot(String id) {
            return index.slotIdx(id) >= 0;
        }

        @Override
        public int tick(Instant t, String path) {
            Horizon horizon = problem.horizon();
            if (t == null) {
                return -1;
            }
            if (!horizon.isAligned(t)) {
                out.error("TIME_NOT_ALIGNED", path, t + " is not on a " + horizon.tickSeconds() + "s tick boundary",
                        args("instant", t.toString(), "tickSeconds", horizon.tickSeconds()));
                return -1;
            }
            if (t.isBefore(horizon.start()) || t.isAfter(horizon.end())) {
                out.error("TIME_OUT_OF_HORIZON", path, t + " is outside the horizon", args("instant", t.toString()));
                return -1;
            }
            return horizon.toTick(t);
        }
    }
}
