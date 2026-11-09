package eu.isygoit.plan.engine;

import eu.isygoit.plan.domain.PlanningProblem;
import eu.isygoit.plan.domain.SolveResult;
import eu.isygoit.plan.domain.SolverOptions;

public interface SolverEngine {
    String name();

    SolveResult solve(PlanningProblem problem, SolverOptions options);

    default SolveResult solve(
            PlanningProblem problem,
            SolverOptions options,
            SolverCancellationToken cancellationToken) {
        return solve(problem, options);
    }
}
