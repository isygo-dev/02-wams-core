package eu.isygoit.solver.spi;

/**
 * Int-indexed read-only view of a finished solution, built ONCE per validation (design 5.2). Indices are the
 * {@code CandidateIndex} indices (request order). All times are integer ticks (tick 0 = horizon start).
 * <p>
 * Implemented by {@code SolutionViewImpl} (WP6); consumed by checkers and evaluators. Immutable once built, thus
 * thread-safe. For an unselected task {@code start}/{@code end} are unspecified and {@code resourceCount} is 0.
 */
public interface SolutionView {

    /** @return number of tasks (equals {@code CandidateIndex.taskCount}) */
    int taskCount();

    /** @return {@code true} when the task is part of the solution (always true for a required task) */
    boolean selected(int task);

    /** @return start tick of a selected task */
    int start(int task);

    /** @return end tick (start + the duration actually used, resource specific) of a selected task */
    int end(int task);

    /** @return number of resources assigned to the task */
    int resourceCount(int task);

    /** @return resource index of the {@code k}-th assigned resource of the task, {@code 0 <= k < resourceCount(task)} */
    int resourceAt(int task, int k);

    /** @return role label of the {@code k}-th assigned resource */
    String roleAt(int task, int k);

    /** @return capacity amount consumed on the {@code k}-th assigned resource */
    int amountAt(int task, int k);

    /**
     * @return selected tasks assigned to the resource, ascending start (ties: task index); never {@code null}; the
     * returned array must not be modified by callers
     */
    int[] tasksOfResource(int resource);

    /** @return slot index chosen by the task start (slot mode), or {@code -1} */
    int slotOf(int task);
}
