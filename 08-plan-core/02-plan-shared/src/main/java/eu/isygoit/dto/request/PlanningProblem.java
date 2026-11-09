package eu.isygoit.dto.request;

import eu.isygoit.util.CollectionCopies;

import java.util.List;

/**
 * The normalized, solver-ready form of a {@link PlanningRequest}, produced by the domain normalizer: all defaults
 * are materialised (requirements, scope, tier, scale, durations from slot length), the engine alias is resolved,
 * preprocess-type constraints are applied as task rewrites and the automatic objective is injected.
 * Lists keep REQUEST ORDER (no sorting, no hash iteration), which makes model building deterministic.
 * <p>
 * {@code jobId} is the database id of the persisted plan job being processed; it is {@code null} when the problem
 * is built without a persisted job (dry-run validation, programmatic solves, tests). {@code timezone} defaults to
 * "UTC", {@code options} to {@link SolverOptions#defaults()} and {@code initialSolution} to an empty solution.
 *
 * @param jobId           persisted job id or null
 * @param domainName      free label of the business domain
 * @param timezone        IANA zone id (meta.timezone)
 * @param horizon         the time grid
 * @param slots           slots in request order
 * @param calendars       calendars in request order
 * @param setupMatrices   setup matrices in request order
 * @param resources       resources in request order
 * @param tasks           tasks in request order
 * @param constraints     constraints in request order
 * @param objectives      objectives in request order (plus injected ones)
 * @param options         solver options with defaults applied
 * @param initialSolution warm start / locks (never null, possibly empty)
 */
public record PlanningProblem(
        Long jobId,
        String domainName,
        String timezone,
        Horizon horizon,
        List<Slot> slots,
        List<Calendar> calendars,
        List<SetupMatrix> setupMatrices,
        List<Resource> resources,
        List<Task> tasks,
        List<Constraint> constraints,
        List<Objective> objectives,
        SolverOptions options,
        InitialSolution initialSolution) {

    public PlanningProblem {
        timezone = timezone == null || timezone.isBlank() ? PlanningMeta.DEFAULT_TIMEZONE : timezone;
        slots = CollectionCopies.list(slots);
        calendars = CollectionCopies.list(calendars);
        setupMatrices = CollectionCopies.list(setupMatrices);
        resources = CollectionCopies.list(resources);
        tasks = CollectionCopies.list(tasks);
        constraints = CollectionCopies.list(constraints);
        objectives = CollectionCopies.list(objectives);
        options = options == null ? SolverOptions.defaults() : options;
        initialSolution = initialSolution == null ? new InitialSolution(null) : initialSolution;
    }
}
