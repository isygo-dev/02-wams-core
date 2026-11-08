package eu.isygoit.plan.engine.timefold;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import eu.isygoit.plan.domain.Resource;
import eu.isygoit.plan.domain.Slot;

import java.util.List;

@PlanningSolution
public class TimefoldPlan {
    private List<Resource> resources = List.of();
    private List<Slot> slots = List.of();
    private List<TimefoldTaskAssignment> assignments = List.of();
    private HardSoftScore score;

    public TimefoldPlan() {
    }

    public TimefoldPlan(List<Resource> resources, List<Slot> slots, List<TimefoldTaskAssignment> assignments) {
        this.resources = resources;
        this.slots = slots;
        this.assignments = assignments;
    }

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "resourceRange")
    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "slotRange")
    public List<Slot> getSlots() {
        return slots;
    }

    public void setSlots(List<Slot> slots) {
        this.slots = slots;
    }

    @PlanningEntityCollectionProperty
    public List<TimefoldTaskAssignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<TimefoldTaskAssignment> assignments) {
        this.assignments = assignments;
    }

    @PlanningScore
    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }
}
