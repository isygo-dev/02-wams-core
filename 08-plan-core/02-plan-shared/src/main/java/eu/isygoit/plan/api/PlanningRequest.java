package eu.isygoit.plan.api;

import eu.isygoit.plan.domain.*;

import java.util.List;

public record PlanningRequest(
        String tenant,
        PlanningMeta meta,
        DomainDefinition domain,
        List<Slot> slots,
        List<Resource> resources,
        List<Task> tasks,
        List<Constraint> constraints,
        List<Objective> objectives,
        SolverOptions solverOptions) {
}
