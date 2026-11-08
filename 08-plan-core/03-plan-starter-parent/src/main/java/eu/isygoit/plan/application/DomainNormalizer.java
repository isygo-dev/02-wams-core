package eu.isygoit.plan.application;

import eu.isygoit.plan.api.PlanningRequest;
import eu.isygoit.plan.domain.PlanningProblem;
import eu.isygoit.plan.domain.SolverOptions;
import eu.isygoit.plan.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

@Component
public class DomainNormalizer {
    public PlanningProblem normalize(PlanningRequest request) {
        if (request == null || request.meta() == null) {
            throw new InvalidRequestException("INVALID_REQUEST", "Request meta is required");
        }
        SolverOptions options = request.solverOptions() == null
                ? SolverOptions.defaults()
                : request.solverOptions();
        return new PlanningProblem(
                request.meta().jobId(),
                request.meta().domainName(),
                request.tasks(),
                request.resources(),
                request.slots(),
                request.constraints(),
                request.objectives(),
                options);
    }
}
