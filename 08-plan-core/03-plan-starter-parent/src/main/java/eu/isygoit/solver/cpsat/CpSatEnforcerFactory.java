package eu.isygoit.solver.cpsat;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.Literal;
import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.SolverOptions;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.exception.ObjectiveRangeException;
import eu.isygoit.solver.CompiledConstraint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * Creates the {@link Enforcer} of every constraint of one build (design 5.5, D-7, 6.9, 6.10). The mapping of the request
 * enforcement to the contributor-visible mode is:
 * <table>
 *   <caption>Enforcement mapping</caption>
 *   <tr><th>request enforcement</th><th>solve pass</th><th>explain pass</th></tr>
 *   <tr><td>STRICTLY_REQUIRED, CONFIGURABLE_HARD</td><td>HARD, {@code instance() == null}</td>
 *       <td>HARD, instance = the CONSTRAINT assumption literal</td></tr>
 *   <tr><td>RELAXABLE_WITH_AUTHORIZATION, relaxable (authorized, maxRelaxation &gt; 0, allowed by relaxationLevels)</td>
 *       <td>RELAXABLE, instance = shared {@code active_k}; cost {@code 1000 * (1 + priority)} in tier -2</td>
 *       <td>RELAXABLE with a FREE {@code active_k} (no cost: relaxation literals are left free)</td></tr>
 *   <tr><td>RELAXABLE_WITH_AUTHORIZATION, not relaxable</td><td>HARD</td><td>HARD (assumption)</td></tr>
 *   <tr><td>SOFT</td><td>SOFT, fresh "satisfied" literal per instance, {@code weight x (1 - satisfied)} in the constraint tier</td>
 *       <td>not built (soft constraints are omitted)</td></tr>
 * </table>
 * Soft and relaxation bookkeeping ({@link #softInstances()}, {@link #relaxLiterals()}) feeds the build result.
 * <p>
 * Thread-safety: single building thread; the created enforcers are bound to the model of this factory. Units: weights and
 * costs are scaled integers in the tier of their constraint.
 */
public final class CpSatEnforcerFactory {

    /** Base cost of dropping a relaxable constraint; the cost is {@code BASE * (1 + priority)} (design 6.9). */
    public static final long RELAXATION_BASE_COST = 1000L;

    private final CpModel model;
    private final TierObjectiveBuilder tiers;
    private final AssumptionRegistry assumptions;
    private final SolverOptions options;
    private final IntConsumer boolVarCounter;
    private final List<SoftInstance> softInstances = new ArrayList<>();
    private final List<RelaxLiteral> relaxLiterals = new ArrayList<>();
    private int softMeasures;

    /**
     * One SOFT unit instance: {@code satisfied} is false exactly when the instance is violated (and then {@code weight} is
     * paid in {@code tier}); for measure penalties ({@link Enforcer#penalizeMeasure}) {@code satisfied} is {@code null}.
     *
     * @param constraintIndex index of the compiled constraint
     * @param constraintId    constraint id
     * @param taskIdx         task index or -1
     * @param resourceIdx     resource index or -1
     * @param satisfied       the literal (null for measure penalties)
     * @param weight          penalty weight
     * @param tier            lexicographic tier
     */
    public record SoftInstance(int constraintIndex, String constraintId, int taskIdx, int resourceIdx,
                               Literal satisfied, int weight, int tier) {
    }

    /**
     * The shared {@code active_k} literal of a relaxable constraint.
     *
     * @param constraintIndex index of the compiled constraint
     * @param constraintId    constraint id
     * @param active          true when the constraint is kept, false when it is dropped
     * @param cost            the tier -2 cost paid when dropped (0 in the explain pass)
     */
    public record RelaxLiteral(int constraintIndex, String constraintId, Literal active, long cost) {
    }

    /**
     * @param model          the model that owns the created literals
     * @param tiers          the objective collector receiving the penalties
     * @param assumptions    the assumption registry of an EXPLAIN build, {@code null} for a solve build
     * @param options        the solver options (relaxation levels)
     * @param boolVarCounter called with the number of new boolean variables for the size guard, may be null
     */
    public CpSatEnforcerFactory(CpModel model, TierObjectiveBuilder tiers, AssumptionRegistry assumptions,
                                SolverOptions options, IntConsumer boolVarCounter) {
        this.model = model;
        this.tiers = tiers;
        this.assumptions = assumptions;
        this.options = options;
        this.boolVarCounter = boolVarCounter;
    }

    /**
     * Whether a constraint is relaxed in one solve (design 6.9): RELAXABLE_WITH_AUTHORIZATION, authorized, maxRelaxation 1
     * and allowed by {@code options.relaxationLevels} (empty = all). Anything else is treated as HARD (v1 semantics).
     *
     * @param constraint the constraint
     * @param options    the solver options
     * @return true when an {@code active_k} literal is created for it
     */
    public static boolean isRelaxable(Constraint constraint, SolverOptions options) {
        if (constraint.enforcement() != IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION
                || constraint.relaxation() == null
                || !constraint.relaxation().authorized()
                || !(constraint.relaxation().maxRelaxation() > 0.0)) {
            return false;
        }
        return options.relaxationLevels().isEmpty() || options.relaxationLevels().contains(constraint.id());
    }

    /**
     * Cost of dropping a relaxable constraint: {@code 1000 * (1 + relaxation.priority)}.
     *
     * @param constraint a relaxable constraint
     * @return the tier -2 cost
     */
    public static long relaxationCost(Constraint constraint) {
        return RELAXATION_BASE_COST * (1L + constraint.relaxation().priority());
    }

    /**
     * Creates the enforcer of one constraint.
     *
     * @param compiled the compiled constraint (must not be a SOFT constraint in an EXPLAIN build)
     * @return the enforcer
     */
    public Enforcer create(CompiledConstraint compiled) {
        Constraint constraint = compiled.constraint();
        if (constraint.enforcement() == IEnumEnforcement.Types.SOFT) {
            if (assumptions != null) {
                throw new IllegalStateException("soft constraints are not built in an explain pass: " + constraint.id());
            }
            return new SoftEnforcer(compiled);
        }
        if (isRelaxable(constraint, options)) {
            return new LiteralEnforcer(Enforcer.Mode.RELAXABLE, relaxLiteral(compiled));
        }
        if (assumptions != null) {
            return new LiteralEnforcer(Enforcer.Mode.HARD,
                    assumptions.literal(AssumptionRegistry.KIND_CONSTRAINT, constraint.id(), "CONSTRAINT"));
        }
        return new LiteralEnforcer(Enforcer.Mode.HARD, null);
    }

    /** @return the SOFT unit instances created so far (unmodifiable view) */
    public List<SoftInstance> softInstances() {
        return Collections.unmodifiableList(softInstances);
    }

    /** @return the relaxation literals created so far (unmodifiable view) */
    public List<RelaxLiteral> relaxLiterals() {
        return Collections.unmodifiableList(relaxLiterals);
    }

    /** @return the number of measure penalties registered through {@link Enforcer#penalizeMeasure} */
    public int softMeasureCount() {
        return softMeasures;
    }

    private Literal relaxLiteral(CompiledConstraint compiled) {
        Constraint constraint = compiled.constraint();
        BoolVar active = model.newBoolVar("active_" + constraint.id());
        if (boolVarCounter != null) {
            boolVarCounter.accept(1);
        }
        long cost = assumptions != null ? 0L : relaxationCost(constraint);
        relaxLiterals.add(new RelaxLiteral(compiled.index(), constraint.id(), active, cost));
        if (assumptions == null) {
            // tier -2: cost x (1 - active)
            tiers.addSoftUnit(TierObjectiveBuilder.TIER_RELAXATION, active, cost);
        }
        return active;
    }

    /** HARD / RELAXABLE enforcer: one shared literal (or none). */
    private static final class LiteralEnforcer implements Enforcer {
        private final Mode mode;
        private final Literal literal;

        LiteralEnforcer(Mode mode, Literal literal) {
            this.mode = mode;
            this.literal = literal;
        }

        @Override
        public Mode mode() {
            return mode;
        }

        @Override
        public Literal instance(int taskIdx, int resourceIdx) {
            return literal;
        }

        @Override
        public boolean penalizeMeasure(LinearExpr measure, long maxMeasure, int taskIdx) {
            return false;
        }

        @Override
        public Literal[] conditions(Literal instance, Literal... extra) {
            return combine(instance, extra);
        }
    }

    /** SOFT enforcer: one fresh "satisfied" literal per instance, penalised in the constraint tier. */
    private final class SoftEnforcer implements Enforcer {
        private final CompiledConstraint compiled;
        private final int weight;
        private final int tier;
        private final Map<Long, Literal> cache = new HashMap<>();

        SoftEnforcer(CompiledConstraint compiled) {
            this.compiled = compiled;
            this.weight = Math.max(1, compiled.constraint().weight());
            this.tier = compiled.constraint().tier() == null ? 0 : compiled.constraint().tier();
        }

        @Override
        public Mode mode() {
            return Mode.SOFT;
        }

        @Override
        public Literal instance(int taskIdx, int resourceIdx) {
            long key = ((long) taskIdx << 32) | (resourceIdx & 0xffffffffL);
            Literal existing = cache.get(key);
            if (existing != null) {
                return existing;
            }
            BoolVar satisfied = model.newBoolVar(""); // unnamed: one literal per violation instance can be many
            if (boolVarCounter != null) {
                boolVarCounter.accept(1);
            }
            tiers.addSoftUnit(tier, satisfied, weight);
            softInstances.add(new SoftInstance(compiled.index(), compiled.constraint().id(), taskIdx, resourceIdx,
                    satisfied, weight, tier));
            cache.put(key, satisfied);
            return satisfied;
        }

        @Override
        public boolean penalizeMeasure(LinearExpr measure, long maxMeasure, int taskIdx) {
            if (maxMeasure < 0) {
                throw new IllegalArgumentException("maxMeasure must not be negative");
            }
            long maxCost;
            try {
                maxCost = Math.multiplyExact((long) weight, maxMeasure);
            } catch (ArithmeticException overflow) {
                throw new ObjectiveRangeException("soft penalty of constraint '" + compiled.constraint().id()
                        + "' overflows: weight " + weight + " x measure " + maxMeasure, overflow);
            }
            tiers.add(tier, LinearExpr.term(measure, weight), 0L, maxCost, compiled.constraint().id());
            softMeasures++;
            softInstances.add(new SoftInstance(compiled.index(), compiled.constraint().id(), taskIdx, -1, null, weight,
                    tier));
            return true;
        }

        @Override
        public Literal[] conditions(Literal instance, Literal... extra) {
            return combine(instance, extra);
        }
    }

    /**
     * Combines the instance literal with the extra conditions, skipping nulls (shared implementation of
     * {@link Enforcer#conditions}).
     */
    static Literal[] combine(Literal instance, Literal... extra) {
        List<Literal> result = new ArrayList<>();
        if (instance != null) {
            result.add(instance);
        }
        if (extra != null) {
            for (Literal literal : extra) {
                if (literal != null) {
                    result.add(literal);
                }
            }
        }
        return result.toArray(new Literal[0]);
    }
}
