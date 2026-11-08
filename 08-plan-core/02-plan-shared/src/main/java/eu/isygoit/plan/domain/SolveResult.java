package eu.isygoit.plan.domain;

import eu.isygoit.enums.IEnumSolverStatus;

import java.util.List;

public record SolveResult(
        IEnumSolverStatus.Types status,
        PlanningSolution solution,
        List<Relaxation> relaxations,
        Diagnostics diagnostics,
        Metrics metrics) {

    public SolveResult {
        relaxations = relaxations == null ? List.of() : List.copyOf(relaxations);
    }
}
