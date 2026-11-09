package eu.isygoit.service.impl.planning;

import eu.isygoit.dto.request.Calendar;
import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.PlanningRequest;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.ResourceRequirement;
import eu.isygoit.dto.request.SetupMatrix;
import eu.isygoit.dto.request.Slot;
import eu.isygoit.dto.request.SolverOptions;
import eu.isygoit.dto.request.Task;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.exception.InvalidRequestException;
import eu.isygoit.exception.ParamsInvalidException;
import eu.isygoit.solver.objective.AutoObjectives;
import eu.isygoit.solver.registry.ConstraintFamilyRegistry;
import eu.isygoit.solver.spi.ConstraintFamily;
import eu.isygoit.solver.spi.TaskRewriter;
import eu.isygoit.util.ParamsBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stage 2 (design 7 / 2.4): turns a validated {@link PlanningRequest} into the solver-ready {@link PlanningProblem}.
 * <p>
 * Steps, in order: (1) HARD constraints whose family is a {@link TaskRewriter} ({@code multi_resource_requirement}) are
 * applied as task rewrites in request order, on the tasks AS DECLARED (before defaults), then the constraint stays in
 * the problem (the compile step marks it inactive); (2) task defaults are materialised: one requirement per task
 * (implicit {@code default} requirement when none), requirement amount = task demand when null, duration from the
 * common slot length when null (slot mode), {@code startSlots} containing {@code ALL} normalised to exactly
 * {@code ["ALL"]}, {@code dueDate} clipped into the horizon; (3) {@link AutoObjectives#inject} adds the automatic
 * selection objective, then every objective gets its tier and {@code scale} (default
 * {@code solverOptions.objectiveScale}); (4) calendars without time zone get the problem zone; (5) setup matrices whose
 * times are all zero are dropped together with the resource references (no circuit is built); (6) the engine alias is
 * already canonical ({@code SolverOptions}).
 * <p>
 * Instants stay instants in the problem: the tick conversion with the {@link Horizon} happens once, in the candidate
 * index builder; alignment was validated before ({@link PlanningRequestValidator}). Lists keep REQUEST ORDER (no
 * sorting, no hash iteration), which makes model building deterministic. A constraint whose type is unknown, or whose
 * params do not bind, is left untouched here: the verifier reports it. Stateless, thread-safe.
 */
@Component
public class DomainNormalizer {

    private static final String ALL = "ALL";

    private final ConstraintFamilyRegistry constraintRegistry;

    /**
     * Creates the normalizer.
     *
     * @param constraintRegistry registry used to find the {@link TaskRewriter} families
     */
    @Autowired
    public DomainNormalizer(ConstraintFamilyRegistry constraintRegistry) {
        this.constraintRegistry = constraintRegistry;
    }

    /**
     * Normalizes a request that is not bound to a persisted job (dry run, programmatic use): the problem carries no
     * job id.
     */
    public PlanningProblem normalize(PlanningRequest request) {
        return normalize(null, request);
    }

    /**
     * Normalizes a request.
     *
     * @param jobId   the id of the persisted plan job being processed, or {@code null} when there is none
     * @param request the request to normalize (already structurally validated)
     * @return the normalized problem
     * @throws InvalidRequestException when the request has no meta or horizon, or a duration cannot be derived
     */
    public PlanningProblem normalize(Long jobId, PlanningRequest request) {
        if (request == null || request.meta() == null) {
            throw new InvalidRequestException("INVALID_REQUEST", "Request meta is required");
        }
        Horizon horizon = request.horizon();
        if (horizon == null) {
            throw new InvalidRequestException("INVALID_HORIZON", "horizon is required");
        }
        SolverOptions options = request.solverOptions() == null ? SolverOptions.defaults() : request.solverOptions();
        String timezone = request.meta().timezone();

        List<Task> rewritten = applyRewriters(request);
        List<Task> tasks = new ArrayList<>(rewritten.size());
        for (Task task : rewritten) {
            tasks.add(materialise(task, request.slots(), horizon));
        }

        List<Objective> injected = AutoObjectives.inject(tasks, request.objectives());
        List<Objective> objectives = new ArrayList<>(injected.size());
        for (Objective objective : injected) {
            objectives.add(new Objective(objective.id(), objective.type(), objective.weight(), objective.tier(),
                    objective.scale() == null ? options.objectiveScale() : objective.scale(),
                    objective.scope(), objective.params()));
        }

        List<Calendar> calendars = new ArrayList<>(request.calendars().size());
        for (Calendar calendar : request.calendars()) {
            calendars.add(calendar.timezone() == null || calendar.timezone().isBlank()
                    ? new Calendar(calendar.id(), calendar.name(), timezone, calendar.windows(), calendar.recurring(),
                    calendar.exceptions(), calendar.breaks())
                    : calendar);
        }

        Set<String> zeroMatrices = new HashSet<>();
        List<SetupMatrix> matrices = new ArrayList<>();
        for (SetupMatrix matrix : request.setupMatrices()) {
            if (allZero(matrix)) {
                zeroMatrices.add(matrix.id());
            } else {
                matrices.add(matrix);
            }
        }
        List<Resource> resources = new ArrayList<>(request.resources().size());
        for (Resource resource : request.resources()) {
            resources.add(resource.setupMatrixId() != null && zeroMatrices.contains(resource.setupMatrixId())
                    ? withoutMatrix(resource) : resource);
        }

        return new PlanningProblem(jobId, request.meta().domainName(), timezone, horizon, request.slots(), calendars,
                matrices, resources, tasks, request.constraints(), objectives, options, request.initialSolution());
    }

    // ------------------------------------------------------------------------------------------ rewriting

    private List<Task> applyRewriters(PlanningRequest request) {
        List<Task> tasks = new ArrayList<>(request.tasks());
        List<Constraint> constraints = request.constraints();
        for (int i = 0; i < constraints.size(); i++) {
            Constraint constraint = constraints.get(i);
            if (!isHard(constraint.enforcement()) || constraint.type() == null) {
                continue;
            }
            ConstraintFamily family = constraintRegistry.find(constraint.type()).orElse(null);
            if (!(family instanceof TaskRewriter rewriter)) {
                continue;
            }
            Object params;
            try {
                params = ParamsBinder.bind(constraint.params(), family.paramsType(), "constraints[" + i + "].params");
            } catch (ParamsInvalidException e) {
                continue; // reported by the verifier
            }
            for (int t = 0; t < tasks.size(); t++) {
                tasks.set(t, rewriter.rewrite(tasks.get(t), constraint, params));
            }
        }
        return tasks;
    }

    /**
     * @param enforcement the constraint enforcement
     * @return {@code true} for STRICTLY_REQUIRED and CONFIGURABLE_HARD
     */
    static boolean isHard(IEnumEnforcement.Types enforcement) {
        return enforcement == IEnumEnforcement.Types.STRICTLY_REQUIRED
                || enforcement == IEnumEnforcement.Types.CONFIGURABLE_HARD;
    }

    // ------------------------------------------------------------------------------------------ defaults

    private Task materialise(Task task, List<Slot> slots, Horizon horizon) {
        int demand = task.demand() == null ? 1 : task.demand();
        List<ResourceRequirement> requirements = new ArrayList<>();
        for (ResourceRequirement requirement : task.effectiveRequirements()) {
            requirements.add(new ResourceRequirement(requirement.role(), requirement.selector(), requirement.count(),
                    requirement.amount() == null ? demand : requirement.amount()));
        }
        List<String> startSlots = task.startSlots();
        if (startSlots.stream().anyMatch(ALL::equalsIgnoreCase)) {
            startSlots = List.of(ALL);
        }
        Integer duration = task.duration();
        if (duration == null) {
            duration = slotLength(startSlots, slots, horizon);
            if (duration == null) {
                throw new InvalidRequestException("DURATION_REQUIRED",
                        "task " + task.id() + ": duration is required unless all listed slots have the same length");
            }
        }
        Instant dueDate = task.dueDate();
        if (dueDate != null) {
            if (dueDate.isBefore(horizon.start())) {
                dueDate = horizon.start();
            } else if (dueDate.isAfter(horizon.end())) {
                dueDate = horizon.end();
            }
        }
        return new Task(task.id(), task.typeId(), task.name(), duration, task.resourceDurations(), demand,
                task.priority(), task.required(), task.prize(), requirements, task.release(), task.deadline(), dueDate,
                startSlots, task.pin(), task.predecessors(), task.setupClass(), task.tags(), task.preferences(),
                task.attributes());
    }

    /** Common length in ticks of the listed slots, or {@code null} when none or different. */
    private static Integer slotLength(List<String> startSlots, List<Slot> slots, Horizon horizon) {
        if (startSlots.isEmpty()) {
            return null;
        }
        Map<String, Slot> byId = new HashMap<>();
        for (Slot slot : slots) {
            byId.putIfAbsent(slot.id(), slot);
        }
        List<Slot> listed = new ArrayList<>();
        if (startSlots.stream().anyMatch(ALL::equalsIgnoreCase)) {
            listed.addAll(slots);
        } else {
            for (String id : startSlots) {
                Slot slot = byId.get(id);
                if (slot != null) {
                    listed.add(slot);
                }
            }
        }
        long length = -1;
        for (Slot slot : listed) {
            long ticks = Duration.between(slot.start(), slot.end()).getSeconds() / horizon.tickSeconds();
            if (length >= 0 && length != ticks) {
                return null;
            }
            length = ticks;
        }
        return length < 0 ? null : (int) Math.min(length, Integer.MAX_VALUE);
    }

    private static boolean allZero(SetupMatrix matrix) {
        if (matrix.defaultTicks() != 0 || matrix.sameClassTicks() != 0) {
            return false;
        }
        return matrix.entries().stream().allMatch(entry -> entry.ticks() == 0);
    }

    private static Resource withoutMatrix(Resource r) {
        return new Resource(r.id(), r.typeId(), r.name(), r.capacity(), r.skills(), r.tags(), r.calendarId(),
                r.unavailable(), r.costPerTick(), r.costPerUse(), r.maxLoadTicks(), null, r.attributes());
    }
}
