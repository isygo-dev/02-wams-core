package eu.isygoit.solver.timefold;

/**
 * Immutable, int-indexed option table shared by every Timefold entity (design 5.6).
 * <p>
 * An OPTION is one (candidate, slot) pair of a task: the task takes the candidate's resource and starts at the slot start.
 * Options of a task are stored contiguously (CSR): the options of task {@code t} are the ids
 * {@code [taskOptionStart(t), taskOptionStart(t+1))}, so the planning variable value range of a task is a plain int range
 * (no list, no resource x slot map per entity).
 * <p>
 * Created by {@link SlotOptionsBuilder} (base table, neutral pruning applied) and by {@link TimefoldBuildContext#freeze()}
 * (compacted table with the penalties/rewards/capacities of the contributors). Never modified after construction, so it
 * is shared by reference between the solution, every entity, every solution clone and every move thread: thread-safe.
 * Units: time in ticks (tick 0 = horizon start), scores in scaled long units (see {@code ScaledMath}).
 */
public final class SlotOptions {

    private final int taskCount;
    private final int slotCount;
    private final int[] taskOptionStart;
    private final int[] optCandidate;
    private final int[] optResource;
    private final int[] optSlot;
    private final int[] optStart;
    private final int[] optEnd;
    private final int[] optDemand;
    private final int[] optCapacity;
    private final long[] optPenalty;
    private final long[] taskReward;

    /**
     * @param taskCount       number of tasks of the index
     * @param slotCount       number of slots of the index
     * @param taskOptionStart CSR offsets, length {@code taskCount + 1}
     * @param optCandidate    [option] candidate id of the index
     * @param optResource     [option] resource index
     * @param optSlot         [option] slot index
     * @param optStart        [option] start tick (the slot start)
     * @param optEnd          [option] end tick (start + duration of the candidate)
     * @param optDemand       [option] capacity consumed on the resource while the slot is occupied (0 for a milestone)
     * @param optCapacity     [option] effective capacity of the (resource, slot) of the option
     * @param optPenalty      [option] soft penalty, scaled
     * @param taskReward      [task] reward when the task is selected, scaled
     */
    SlotOptions(int taskCount, int slotCount, int[] taskOptionStart, int[] optCandidate, int[] optResource,
                int[] optSlot, int[] optStart, int[] optEnd, int[] optDemand, int[] optCapacity,
                long[] optPenalty, long[] taskReward) {
        this.taskCount = taskCount;
        this.slotCount = slotCount;
        this.taskOptionStart = taskOptionStart;
        this.optCandidate = optCandidate;
        this.optResource = optResource;
        this.optSlot = optSlot;
        this.optStart = optStart;
        this.optEnd = optEnd;
        this.optDemand = optDemand;
        this.optCapacity = optCapacity;
        this.optPenalty = optPenalty;
        this.taskReward = taskReward;
    }

    /** @return number of tasks (entities may exist for fewer: tasks without option have none) */
    public int taskCount() {
        return taskCount;
    }

    /** @return number of slots of the index */
    public int slotCount() {
        return slotCount;
    }

    /** @return total number of options */
    public int optionCount() {
        return optCandidate.length;
    }

    /** @return first option id of the task */
    public int taskOptionStart(int task) {
        return taskOptionStart[task];
    }

    /** @return one past the last option id of the task */
    public int taskOptionEnd(int task) {
        return taskOptionStart[task + 1];
    }

    /** @return number of options of the task */
    public int taskOptionCount(int task) {
        return taskOptionStart[task + 1] - taskOptionStart[task];
    }

    /** @return the candidate id (index) of the option */
    public int candidate(int option) {
        return optCandidate[option];
    }

    /** @return the resource index of the option */
    public int resource(int option) {
        return optResource[option];
    }

    /** @return the slot index of the option */
    public int slot(int option) {
        return optSlot[option];
    }

    /** @return the start tick of the option */
    public int start(int option) {
        return optStart[option];
    }

    /** @return the end tick of the option */
    public int end(int option) {
        return optEnd[option];
    }

    /** @return the capacity consumed while the option is chosen (0 for a zero-duration task) */
    public int demand(int option) {
        return optDemand[option];
    }

    /** @return the effective capacity of the (resource, slot) of the option */
    public int capacity(int option) {
        return optCapacity[option];
    }

    /** @return the key identifying the (resource, slot) pair of the option: equal keys share one capacity bucket */
    public long capacityKey(int option) {
        return (long) optResource[option] * slotCount + optSlot[option];
    }

    /** @return the scaled soft penalty of the option */
    public long penalty(int option) {
        return optPenalty[option];
    }

    /** @return the scaled reward when the task is selected */
    public long reward(int task) {
        return taskReward[task];
    }

    /**
     * @param option an option id
     * @return the task index owning the option (binary search over the CSR offsets; not for hot loops)
     */
    public int taskOf(int option) {
        int lo = 0;
        int hi = taskCount - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (taskOptionStart[mid] <= option) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return lo;
    }

    /** @return a defensive copy of the soft penalties (tests, diagnostics) */
    public long[] penaltiesCopy() {
        return optPenalty.clone();
    }
}
