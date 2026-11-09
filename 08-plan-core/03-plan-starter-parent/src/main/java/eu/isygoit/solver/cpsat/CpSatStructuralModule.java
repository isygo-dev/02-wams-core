package eu.isygoit.solver.cpsat;

/**
 * Optional plug-in of the CP-SAT builder for structural model parts that are not catalogue constraints (setup circuits,
 * symmetry breaking). Called by {@code CpModelBuilder} in ascending {@link #order()} after the structural model
 * (design 6.1 steps 7-8).
 * <p>
 * Implemented by {@code SetupCircuitModule} (order 700, WP4b) and {@code SymmetryBreakingModule} (order 800, WP3) as Spring
 * beans. Stateless and thread-safe; all state lives in the per-build {@link CpModelContext}.
 */
public interface CpSatStructuralModule {

    /** @return a unique module name (for logs) */
    String name();

    /** @return the position: modules run in ascending order, ties by name */
    int order();

    /** Adds the module's model parts to {@code ctx.model()}; allocations must call {@code ctx.count(..)}. */
    void apply(CpModelContext ctx);
}
