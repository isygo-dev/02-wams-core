package eu.isygoit.dto.request;

import eu.isygoit.util.CollectionCopies;

import java.util.List;

/**
 * Top-level planning request (IR 2.0). Immutable; collections are copied and defaults applied by the compact
 * constructor. Cross-entity rules (references, cycles, units, windows) and the REQUIRED fields ({@code horizon},
 * non-empty {@code resources} and {@code tasks}) are validated by the service layer, never here.
 * <p>
 * The IR mapper used by the validation endpoint binds with {@code FAIL_ON_UNKNOWN_PROPERTIES=true} (typos must
 * fail); only the stored-job re-read is lenient.
 *
 * @param meta           {@code meta.version} MUST be "2.0"
 * @param domain         OPTIONAL UI hints (type ids and attribute names); null becomes an empty definition
 * @param horizon        REQUIRED time grid
 * @param slots          default empty
 * @param calendars      default empty
 * @param setupMatrices  default empty
 * @param resources      REQUIRED non-empty
 * @param tasks          REQUIRED non-empty
 * @param constraints    default empty
 * @param objectives     default empty
 * @param solverOptions  default {@link SolverOptions#defaults()}
 * @param initialSolution OPTIONAL warm start / locked assignments (null = none)
 */
public record PlanningRequest(
        PlanningMeta meta,
        DomainDefinition domain,
        Horizon horizon,
        List<Slot> slots,
        List<Calendar> calendars,
        List<SetupMatrix> setupMatrices,
        List<Resource> resources,
        List<Task> tasks,
        List<Constraint> constraints,
        List<Objective> objectives,
        SolverOptions solverOptions,
        InitialSolution initialSolution) {

    public PlanningRequest {
        domain = domain == null ? new DomainDefinition(null, null, null) : domain;
        slots = CollectionCopies.list(slots);
        calendars = CollectionCopies.list(calendars);
        setupMatrices = CollectionCopies.list(setupMatrices);
        resources = CollectionCopies.list(resources);
        tasks = CollectionCopies.list(tasks);
        constraints = CollectionCopies.list(constraints);
        objectives = CollectionCopies.list(objectives);
        solverOptions = solverOptions == null ? SolverOptions.defaults() : solverOptions;
    }
}
