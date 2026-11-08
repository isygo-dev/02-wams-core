package eu.isygoit.plan.engine.timefold;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import eu.isygoit.plan.domain.Resource;
import eu.isygoit.plan.domain.Slot;
import eu.isygoit.plan.domain.Task;

import java.util.Map;
import java.util.Set;

@PlanningEntity
public class TimefoldTaskAssignment {
    private Task task;
    private Resource resource;
    private Slot slot;
    private Set<AssignmentKey> allowedAssignments = Set.of();
    private Map<AssignmentKey, Long> objectiveCosts = Map.of();
    private Map<AssignmentKey, Integer> softPenalties = Map.of();
    private Map<ResourceSlotKey, Integer> capacities = Map.of();

    public TimefoldTaskAssignment() {
    }

    public TimefoldTaskAssignment(
            Task task,
            Set<AssignmentKey> allowedAssignments,
            Map<AssignmentKey, Long> objectiveCosts,
            Map<AssignmentKey, Integer> softPenalties,
            Map<ResourceSlotKey, Integer> capacities) {
        this.task = task;
        this.allowedAssignments = allowedAssignments;
        this.objectiveCosts = objectiveCosts;
        this.softPenalties = softPenalties;
        this.capacities = capacities;
    }

    @PlanningVariable(valueRangeProviderRefs = "resourceRange", nullable = true)
    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    @PlanningVariable(valueRangeProviderRefs = "slotRange", nullable = true)
    public Slot getSlot() {
        return slot;
    }

    public void setSlot(Slot slot) {
        this.slot = slot;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public boolean isHardAllowed() {
        if (resource == null || slot == null) {
            return !task.requiredTask() && resource == null && slot == null;
        }
        return allowedAssignments.contains(key());
    }

    public int getCapacity() {
        return resource == null || slot == null
                ? 1
                : capacities.getOrDefault(new ResourceSlotKey(resource.id(), slot.id()), 1);
    }

    public long getObjectiveCost() {
        return resource == null || slot == null
                ? 0L
                : objectiveCosts.getOrDefault(key(), 0L);
    }

    public int getSoftPenalty() {
        return resource == null || slot == null
                ? 0
                : softPenalties.getOrDefault(key(), 0);
    }

    private AssignmentKey key() {
        return new AssignmentKey(task.id(), resource.id(), slot.id());
    }
}
