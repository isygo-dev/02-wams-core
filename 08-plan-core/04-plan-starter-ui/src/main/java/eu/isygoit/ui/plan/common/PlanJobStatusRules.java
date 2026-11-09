package eu.isygoit.ui.plan.common;

import eu.isygoit.enums.IEnumJobStatusCode;

/**
 * Single place that decides which actions a planning job offers in each lifecycle status. The job card, the
 * table actions (which reuse the card buttons) and the job page all ask this class, so the gating is never
 * duplicated.
 *
 * <ul>
 *   <li>details, logs, clone: every status</li>
 *   <li>edit request, cancel: CREATED</li>
 *   <li>stop: NORMALIZING, SOLVING</li>
 *   <li>export result: SUCCEEDED, INFEASIBLE</li>
 *   <li>delete: SUCCEEDED, INFEASIBLE, FAILED, CANCELLED (terminal)</li>
 * </ul>
 */
public final class PlanJobStatusRules {

    private PlanJobStatusRules() {
    }

    /**
     * The job reached its final state: nothing more will happen to it.
     */
    public static boolean isTerminal(IEnumJobStatusCode.Types status) {
        return status == IEnumJobStatusCode.Types.SUCCEEDED
                || status == IEnumJobStatusCode.Types.INFEASIBLE
                || status == IEnumJobStatusCode.Types.FAILED
                || status == IEnumJobStatusCode.Types.CANCELLED;
    }

    /**
     * The request can be replaced only while no worker has picked the job up.
     */
    public static boolean isEditable(IEnumJobStatusCode.Types status) {
        return status == IEnumJobStatusCode.Types.CREATED;
    }

    /**
     * A job that has not started can be cancelled.
     */
    public static boolean isCancellable(IEnumJobStatusCode.Types status) {
        return status == IEnumJobStatusCode.Types.CREATED;
    }

    /**
     * A running solver can be asked to stop (the best solution found so far is kept).
     */
    public static boolean isStoppable(IEnumJobStatusCode.Types status) {
        return status == IEnumJobStatusCode.Types.NORMALIZING
                || status == IEnumJobStatusCode.Types.SOLVING;
    }

    /**
     * Only a finished job record can be deleted.
     */
    public static boolean isDeletable(IEnumJobStatusCode.Types status) {
        return isTerminal(status);
    }

    /**
     * A result file exists when the solver ended with a solution or a proof of infeasibility.
     */
    public static boolean isExportable(IEnumJobStatusCode.Types status) {
        return status == IEnumJobStatusCode.Types.SUCCEEDED
                || status == IEnumJobStatusCode.Types.INFEASIBLE;
    }
}
