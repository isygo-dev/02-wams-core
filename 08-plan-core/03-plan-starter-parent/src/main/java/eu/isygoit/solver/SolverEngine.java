package eu.isygoit.solver;

import eu.isygoit.dto.response.SolveResult;

/**
 * Engine contract of Engine v2 (design 5.3): one implementation per solver technology ({@code cp_sat}, {@code timefold}).
 * <p>
 * Implemented by {@code CpSatSolverEngine} (WP3) and {@code TimefoldSolverEngine} (WP5) as Spring beans; called by
 * {@code PlanningService} after compile, pre-solve analysis and concurrency guard. Engines are singletons and must be
 * thread-safe (one solve per call, no shared mutable state). Must not import the other engine's technology: this
 * interface is engine neutral.
 */
public interface SolverEngine {

    /** @return the canonical engine id: {@code "cp_sat"} or {@code "timefold"} (lower-case) */
    String name();

    /** @return what this engine supports: static features plus the registry-derived constraint/objective support (5.6) */
    EngineCapabilities capabilities();

    /**
     * Solves a compiled problem.
     *
     * @param problem the validated, normalized, indexed problem
     * @param context options, cancellation token, listener and the start of the whole request (time budget)
     * @return the result; INFEASIBLE/UNKNOWN are returned as statuses (the service maps INFEASIBLE to its exception)
     */
    SolveResult solve(CompiledProblem problem, SolveContext context);
}
