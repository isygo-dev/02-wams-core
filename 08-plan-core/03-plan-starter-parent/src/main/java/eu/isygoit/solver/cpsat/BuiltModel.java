package eu.isygoit.solver.cpsat;

import com.google.ortools.sat.CpModel;
import eu.isygoit.dto.response.ModelStats;

import java.util.Collections;
import java.util.List;

/**
 * Result of {@link CpModelBuilder#build}: the CP-SAT model together with everything needed to solve it, read the solution
 * and explain an infeasibility.
 * <ul>
 *   <li>{@link #model()} / {@link #context()}: the model and its variable arrays (the solution is read through the context);</li>
 *   <li>{@link #tiers()} / {@link #plan()}: the lexicographic objective terms and the chosen optimisation mode (the model
 *   already carries {@code minimize} for the WEIGHTED / single tier case; SEQUENTIAL is driven by the {@code SolveRunner});</li>
 *   <li>{@link #softInstances()} / {@link #relaxLiterals()}: soft and relaxation bookkeeping;</li>
 *   <li>{@link #assumptions()}: the assumption descriptors of an EXPLAIN build (already installed on the model with
 *   {@code addAssumptions}; the explainer may re-install subsets with {@link AssumptionRegistry#install});</li>
 *   <li>{@link #explainSkipped()}: constraint ids that exceeded a pairwise guard in the EXPLAIN build and are therefore not
 *   part of the core ({@code diagnostics.details.explainSkipped}).</li>
 * </ul>
 * Thread-safety: not thread-safe; produced by one thread and consumed by one thread afterwards. Units: ticks.
 */
public final class BuiltModel {

    private final CpModelContext context;
    private final BuildMode mode;
    private final TierObjectiveBuilder tiers;
    private final TierObjectiveBuilder.Plan plan;
    private final List<CpSatEnforcerFactory.SoftInstance> softInstances;
    private final List<CpSatEnforcerFactory.RelaxLiteral> relaxLiterals;
    private final List<String> explainSkipped;
    private final boolean hasHints;
    private final boolean hasObjective;
    private final long buildNanos;

    BuiltModel(CpModelContext context, BuildMode mode, TierObjectiveBuilder tiers, TierObjectiveBuilder.Plan plan,
               List<CpSatEnforcerFactory.SoftInstance> softInstances,
               List<CpSatEnforcerFactory.RelaxLiteral> relaxLiterals, List<String> explainSkipped, boolean hasHints,
               boolean hasObjective, long buildNanos) {
        this.context = context;
        this.mode = mode;
        this.tiers = tiers;
        this.plan = plan;
        this.softInstances = List.copyOf(softInstances);
        this.relaxLiterals = List.copyOf(relaxLiterals);
        this.explainSkipped = List.copyOf(explainSkipped);
        this.hasHints = hasHints;
        this.hasObjective = hasObjective;
        this.buildNanos = buildNanos;
    }

    /** @return the CP-SAT model */
    public CpModel model() {
        return context.model();
    }

    /** @return the model context holding the variable arrays */
    public CpModelContext context() {
        return context;
    }

    /** @return SOLVE or EXPLAIN */
    public BuildMode mode() {
        return mode;
    }

    /** @return the assumption descriptors (empty in SOLVE builds) */
    public List<AssumptionRef> assumptions() {
        return context.assumptions == null ? List.of() : context.assumptions.refs();
    }

    /** @return the assumption registry of an EXPLAIN build, {@code null} for SOLVE builds */
    public AssumptionRegistry assumptionRegistry() {
        return context.assumptions;
    }

    /** @return the objective terms by tier (empty when the problem has no objective term) */
    public TierObjectiveBuilder tiers() {
        return tiers;
    }

    /** @return the optimisation plan, or {@code null} when there is no objective term (and in EXPLAIN builds) */
    public TierObjectiveBuilder.Plan plan() {
        return plan;
    }

    /** @return true when {@code minimize} was set on the model (WEIGHTED or single tier) */
    public boolean hasObjective() {
        return hasObjective;
    }

    /** @return true when the model carries hints (so {@code repair_hint} is worth enabling) */
    public boolean hasHints() {
        return hasHints;
    }

    /** @return the SOFT instances (unit literals and measure penalties) in creation order */
    public List<CpSatEnforcerFactory.SoftInstance> softInstances() {
        return softInstances;
    }

    /** @return the relaxation literals {@code active_k} in creation order */
    public List<CpSatEnforcerFactory.RelaxLiteral> relaxLiterals() {
        return relaxLiterals;
    }

    /** @return ids of the constraints skipped in an EXPLAIN build because of a size guard */
    public List<String> explainSkipped() {
        return Collections.unmodifiableList(explainSkipped);
    }

    /** @return the model construction time in nanoseconds (excluding the index build) */
    public long buildNanos() {
        return buildNanos;
    }

    /** @return the model statistics (O(model size)) */
    public ModelStats stats() {
        return context.stats();
    }
}
