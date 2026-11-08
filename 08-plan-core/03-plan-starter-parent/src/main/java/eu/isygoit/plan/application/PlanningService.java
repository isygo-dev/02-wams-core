package eu.isygoit.plan.application;

import eu.isygoit.plan.api.PlanningRequest;
import eu.isygoit.plan.domain.PlanningProblem;
import eu.isygoit.plan.domain.SolveResult;
import eu.isygoit.plan.engine.ConstraintRegistry;
import eu.isygoit.plan.engine.ObjectiveRegistry;
import eu.isygoit.plan.engine.SolverRegistry;
import eu.isygoit.plan.exception.InfeasibleException;
import org.springframework.stereotype.Service;

@Service
public class PlanningService {
    private final PlanningRequestValidator requestValidator;
    private final DomainNormalizer normalizer;
    private final ConstraintRegistry constraintRegistry;
    private final ObjectiveRegistry objectiveRegistry;
    private final SolverRegistry solverRegistry;
    private final PlanningSolutionValidator solutionValidator;

    public PlanningService(
            PlanningRequestValidator requestValidator,
            DomainNormalizer normalizer,
            ConstraintRegistry constraintRegistry,
            ObjectiveRegistry objectiveRegistry,
            SolverRegistry solverRegistry,
            PlanningSolutionValidator solutionValidator) {
        this.requestValidator = requestValidator;
        this.normalizer = normalizer;
        this.constraintRegistry = constraintRegistry;
        this.objectiveRegistry = objectiveRegistry;
        this.solverRegistry = solverRegistry;
        this.solutionValidator = solutionValidator;
    }

    public SolveResult solve(PlanningRequest request) {
        PlanningProblem problem = normalizeAndValidate(request);
        SolveResult result = solverRegistry.get(problem.options().engine()).solve(problem, problem.options());
        if (result.status() == eu.isygoit.enums.IEnumSolverStatus.Types.INFEASIBLE) {
            throw new InfeasibleException(result.diagnostics());
        }
        if (result.solution() != null) {
            solutionValidator.validate(problem, result);
        }
        return result;
    }

    public PlanningProblem normalizeAndValidate(PlanningRequest request) {
        requestValidator.validate(request);
        PlanningProblem problem = normalizer.normalize(request);
        constraintRegistry.verifyAll(problem);
        objectiveRegistry.verifyAll(problem.objectives());
        return problem;
    }
}
