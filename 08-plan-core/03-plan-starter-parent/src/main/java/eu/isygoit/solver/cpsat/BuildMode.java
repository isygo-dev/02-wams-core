package eu.isygoit.solver.cpsat;

/**
 * Mode of {@code CpModelBuilder.build(CompiledProblem, SolveContext, BuildMode)} (design 6.1).
 */
public enum BuildMode {
    /** Normal solve model: HARD constraints are unconditional, domains are tight. */
    SOLVE,
    /** Infeasibility-explanation model: domain restrictions and HARD constraints become assumption-enforced constraints. */
    EXPLAIN
}
