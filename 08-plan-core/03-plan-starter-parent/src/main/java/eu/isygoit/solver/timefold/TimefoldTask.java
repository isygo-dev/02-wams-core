package eu.isygoit.solver.timefold;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeFactory;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

/**
 * Planning entity of the Timefold subset (design 5.6): ONE task with ONE composite planning variable, the OPTION
 * (a (candidate, slot) pair, so resource and slot are decided together, which cuts the search space).
 * <p>
 * The value range is an int range {@code [taskOptionStart, taskOptionEnd)} over the shared immutable {@link SlotOptions}:
 * no per-entity list and no resource x slot map. {@code option == null} means "not selected" (legal only for optional
 * tasks; a required task left unassigned costs one hard point). All derived getters are pure functions of the option and
 * the shared table, so entity clones and move threads see identical data.
 * <p>
 * Created by {@link TimefoldSolverEngine} (one per task that has at least one option); mutated only by the solver.
 * Not thread-safe by itself (Timefold owns it during a solve). Units: ticks and scaled longs, see {@link SlotOptions}.
 */
@PlanningEntity
public class TimefoldTask {

    private int taskIdx;
    private boolean required;
    private SlotOptions options;
    private Integer option;

    /** Required by Timefold. */
    public TimefoldTask() {
    }

    /**
     * @param taskIdx  task index in the candidate index
     * @param required {@code true} for a required task
     * @param options  the frozen shared option table
     */
    public TimefoldTask(int taskIdx, boolean required, SlotOptions options) {
        this.taskIdx = taskIdx;
        this.required = required;
        this.options = options;
    }

    /** @return the task index in the candidate index */
    public int getTaskIdx() {
        return taskIdx;
    }

    public void setTaskIdx(int taskIdx) {
        this.taskIdx = taskIdx;
    }

    /** @return whether the task is required */
    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    /** @return the shared immutable option table */
    public SlotOptions getOptions() {
        return options;
    }

    public void setOptions(SlotOptions options) {
        this.options = options;
    }

    /** @return the int range of this task's option ids (a view, no allocation of values) */
    // TIMEFOLD-VERIFY: entity-level @ValueRangeProvider on a getter returning CountableValueRange<Integer>, referenced by id.
    @ValueRangeProvider(id = "optionRange")
    public CountableValueRange<Integer> getOptionRange() {
        return ValueRangeFactory.createIntValueRange(options.taskOptionStart(taskIdx), options.taskOptionEnd(taskIdx));
    }

    /** @return the chosen option id, or {@code null} when the task is not selected */
    // TIMEFOLD-VERIFY: allowsUnassigned (1.x name; replaces the deprecated nullable) on a boxed Integer variable.
    @PlanningVariable(valueRangeProviderRefs = "optionRange", allowsUnassigned = true)
    public Integer getOption() {
        return option;
    }

    public void setOption(Integer option) {
        this.option = option;
    }

    /** @return capacity consumed by the chosen option, 0 when unassigned */
    public int getDemand() {
        return option == null ? 0 : options.demand(option);
    }

    /** @return the (resource, slot) bucket key of the chosen option; only call when assigned */
    public long getCapacityKey() {
        return options.capacityKey(option);
    }

    /** @return the capacity of the chosen option's bucket; only call when assigned */
    public int getSlotCapacity() {
        return options.capacity(option);
    }

    /** @return the scaled soft penalty of the chosen option, 0 when unassigned */
    public long getPenalty() {
        return option == null ? 0L : options.penalty(option);
    }

    /** @return the scaled reward of the selected task, 0 when unassigned */
    public long getReward() {
        return option == null ? 0L : options.reward(taskIdx);
    }
}
