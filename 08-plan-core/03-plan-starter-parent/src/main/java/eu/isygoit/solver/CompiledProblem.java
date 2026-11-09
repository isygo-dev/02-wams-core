package eu.isygoit.solver;

import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.solver.spi.ModelCost;

import java.util.List;

/**
 * The engine input of Engine v2: the normalized problem, its int-indexed {@link CandidateIndex}, the model cost estimate
 * and the constraints/objectives resolved against their families (design 5.3).
 * <p>
 * Created by {@code PlanningService.compile} (WP2); consumed by the pre-solve analyzer, engines and the solution
 * validator. Immutable (lists are unmodifiable copies); the index is immutable after construction. Thread-safe.
 *
 * @param problem     the normalized problem (request order)
 * @param index       the candidate index built once
 * @param estimate    the summed model cost estimate
 * @param constraints one entry per {@code problem.constraints()} element, same order
 * @param objectives  one entry per {@code problem.objectives()} element, same order
 */
public record CompiledProblem(
        PlanningProblem problem,
        CandidateIndex index,
        ModelCost estimate,
        List<CompiledConstraint> constraints,
        List<CompiledObjective> objectives) {

    public CompiledProblem {
        estimate = estimate == null ? ModelCost.ZERO : estimate;
        constraints = constraints == null ? List.of() : List.copyOf(constraints);
        objectives = objectives == null ? List.of() : List.copyOf(objectives);
    }
}
