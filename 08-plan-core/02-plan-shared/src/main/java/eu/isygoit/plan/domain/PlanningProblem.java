package eu.isygoit.plan.domain;

import java.util.List;
import java.util.UUID;

public record PlanningProblem(
        UUID jobId,
        String domainName,
        List<Task> tasks,
        List<Resource> resources,
        List<Slot> slots,
        List<Constraint> constraints,
        List<Objective> objectives,
        SolverOptions options) {

    public PlanningProblem {
        tasks = tasks == null ? List.of() : List.copyOf(tasks);
        resources = resources == null ? List.of() : List.copyOf(resources);
        slots = slots == null ? List.of() : List.copyOf(slots);
        constraints = constraints == null ? List.of() : List.copyOf(constraints);
        objectives = objectives == null ? List.of() : List.copyOf(objectives);
        options = options == null ? SolverOptions.defaults() : options;
    }
}
