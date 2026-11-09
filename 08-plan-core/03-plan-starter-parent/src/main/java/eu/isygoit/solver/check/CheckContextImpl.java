package eu.isygoit.solver.check;

import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.CheckContext;
import eu.isygoit.solver.spi.SolutionView;
import eu.isygoit.util.ScaledMath;

import java.util.Objects;

/**
 * Default {@link CheckContext}: problem + index view + solution view of ONE validation (design 5.2).
 * <p>
 * Immutable; thread-safe as far as its three collaborators are (all read-only). {@link #scaled(double, int)} delegates to
 * {@link ScaledMath}, the single decimal-to-integer arithmetic shared with the engines (round half up, 2^53 guard).
 */
public final class CheckContextImpl implements CheckContext {

    private final PlanningProblem problem;
    private final CandidateIndexView index;
    private final SolutionView solution;

    /**
     * @param problem  the normalized problem
     * @param index    read-only index view (a real CandidateIndex in production)
     * @param solution the solution under validation
     */
    public CheckContextImpl(PlanningProblem problem, CandidateIndexView index, SolutionView solution) {
        this.problem = Objects.requireNonNull(problem, "problem");
        this.index = Objects.requireNonNull(index, "index");
        this.solution = Objects.requireNonNull(solution, "solution");
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
    public SolutionView solution() {
        return solution;
    }

    @Override
    public long scaled(double value, int scale) {
        return ScaledMath.scaleSigned(value, scale);
    }
}
