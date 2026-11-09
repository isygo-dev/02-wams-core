package eu.isygoit.service.impl.planning;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.InitialAssignment;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.ResourceRequirement;
import eu.isygoit.dto.request.Task;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.EngineCapabilities.Feature;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Computes the engine features a normalized problem REQUIRES (design 7.2 / 4.5).
 * <p>
 * The scan is feature based, not field based: a request that merely LISTS a v2 field with its default value (empty
 * predecessors, capacity 1) does not require the feature, so an engine without it still accepts the request; any real
 * use does and is reported with the path of the FIRST offender. Rules: {@code FREE_START} = a task with empty
 * {@code startSlots}; {@code SLOT_MODE} = a task with slots; {@code RESOURCE_DURATIONS}; {@code CUMULATIVE_DEMAND} = a
 * capacity above 1 or an amount above 1; {@code OPTIONAL_TASKS}; {@code PRECEDENCES} = predecessors or a
 * {@code precedence} constraint; {@code TIME_WINDOWS} = release/deadline or a {@code time_window} constraint;
 * {@code CALENDARS} = calendar or unavailability; {@code SETUPS}; {@code MULTI_RESOURCE} = count above 1, more than one
 * requirement or a {@code multi_resource_requirement} constraint; {@code PINS} = pin or {@code fixed_assignment};
 * {@code INITIAL_SOLUTION}; {@code LOCKED}; {@code LEXICOGRAPHIC} = more than one distinct tier among enabled
 * objectives and SOFT constraints (the reserved negative tiers of the built-in selection value and relaxation do not
 * count); {@code RELAXATION} = an authorized relaxable constraint; {@code COST_PER_USE}; {@code MAX_LOAD_TICKS};
 * {@code DETERMINISTIC}. {@code EXPLAIN_ASSUMPTIONS} is informational and is NOT required (the verifier turns a missing
 * support into a warning). Pure static function, thread-safe.
 */
public final class FeatureScanner {

    private FeatureScanner() {
    }

    /**
     * Scans a normalized problem.
     *
     * @param problem the normalized problem
     * @return the required features, each mapped to the JSON path of its first offender, in enum order
     */
    public static Map<Feature, String> scan(PlanningProblem problem) {
        Map<Feature, String> required = new EnumMap<>(Feature.class);
        List<Task> tasks = problem.tasks();
        for (int t = 0; t < tasks.size(); t++) {
            Task task = tasks.get(t);
            String at = "tasks[" + t + "]";
            if (task.startSlots().isEmpty()) {
                required.putIfAbsent(Feature.FREE_START, at + ".startSlots");
            } else {
                required.putIfAbsent(Feature.SLOT_MODE, at + ".startSlots");
            }
            if (!task.resourceDurations().isEmpty()) {
                required.putIfAbsent(Feature.RESOURCE_DURATIONS, at + ".resourceDurations");
            }
            if (!task.requiredTask()) {
                required.putIfAbsent(Feature.OPTIONAL_TASKS, at + ".required");
            }
            if (!task.predecessors().isEmpty()) {
                required.putIfAbsent(Feature.PRECEDENCES, at + ".predecessors");
            }
            if (task.release() != null) {
                required.putIfAbsent(Feature.TIME_WINDOWS, at + ".release");
            }
            if (task.deadline() != null) {
                required.putIfAbsent(Feature.TIME_WINDOWS, at + ".deadline");
            }
            if (task.pin() != null) {
                required.putIfAbsent(Feature.PINS, at + ".pin");
            }
            List<ResourceRequirement> requirements = task.effectiveRequirements();
            if (requirements.size() > 1) {
                required.putIfAbsent(Feature.MULTI_RESOURCE, at + ".requirements");
            }
            for (int q = 0; q < requirements.size(); q++) {
                ResourceRequirement requirement = requirements.get(q);
                String rq = at + ".requirements[" + q + "]";
                if (requirement.count() != null && requirement.count() > 1) {
                    required.putIfAbsent(Feature.MULTI_RESOURCE, rq + ".count");
                }
                int amount = requirement.amount() != null ? requirement.amount()
                        : (task.demand() == null ? 1 : task.demand());
                if (amount > 1) {
                    required.putIfAbsent(Feature.CUMULATIVE_DEMAND, rq + ".amount");
                }
            }
        }
        List<Resource> resources = problem.resources();
        for (int r = 0; r < resources.size(); r++) {
            Resource resource = resources.get(r);
            String at = "resources[" + r + "]";
            if (resource.capacity() != null && resource.capacity() > 1) {
                required.putIfAbsent(Feature.CUMULATIVE_DEMAND, at + ".capacity");
            }
            if (resource.calendarId() != null) {
                required.putIfAbsent(Feature.CALENDARS, at + ".calendarId");
            }
            if (!resource.unavailable().isEmpty()) {
                required.putIfAbsent(Feature.CALENDARS, at + ".unavailable");
            }
            if (resource.setupMatrixId() != null) {
                required.putIfAbsent(Feature.SETUPS, at + ".setupMatrixId");
            }
            if (resource.costPerUse() != null && resource.costPerUse() > 0) {
                required.putIfAbsent(Feature.COST_PER_USE, at + ".costPerUse");
            }
            if (resource.maxLoadTicks() != null) {
                required.putIfAbsent(Feature.MAX_LOAD_TICKS, at + ".maxLoadTicks");
            }
        }
        Set<Integer> tiers = new HashSet<>();
        List<Constraint> constraints = problem.constraints();
        for (int c = 0; c < constraints.size(); c++) {
            Constraint constraint = constraints.get(c);
            String at = "constraints[" + c + "]";
            switch (String.valueOf(constraint.type())) {
                case "precedence":
                    required.putIfAbsent(Feature.PRECEDENCES, at);
                    break;
                case "time_window":
                    required.putIfAbsent(Feature.TIME_WINDOWS, at);
                    break;
                case "multi_resource_requirement":
                    required.putIfAbsent(Feature.MULTI_RESOURCE, at);
                    break;
                case "fixed_assignment":
                    required.putIfAbsent(Feature.PINS, at);
                    break;
                default:
                    break;
            }
            if (constraint.enforcement() == IEnumEnforcement.Types.SOFT) {
                tiers.add(constraint.tier() == null ? 0 : constraint.tier());
            } else if (constraint.enforcement() == IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION
                    && constraint.relaxation() != null && constraint.relaxation().authorized()) {
                required.putIfAbsent(Feature.RELAXATION, at);
            }
        }
        for (var objective : problem.objectives()) {
            if (objective.weight() > 0) {
                tiers.add(objective.tier() == null ? 0 : objective.tier());
            }
        }
        tiers.removeIf(tier -> tier < 0);
        if (tiers.size() > 1) {
            required.putIfAbsent(Feature.LEXICOGRAPHIC, "objectives");
        }
        if (!problem.initialSolution().assignments().isEmpty()) {
            required.putIfAbsent(Feature.INITIAL_SOLUTION, "initialSolution");
            List<InitialAssignment> assignments = problem.initialSolution().assignments();
            for (int i = 0; i < assignments.size(); i++) {
                if (assignments.get(i).locked()) {
                    required.putIfAbsent(Feature.LOCKED, "initialSolution.assignments[" + i + "]");
                    break;
                }
            }
        }
        if (Boolean.TRUE.equals(problem.options().deterministic())) {
            required.putIfAbsent(Feature.DETERMINISTIC, "solverOptions.deterministic");
        }
        return required;
    }
}
