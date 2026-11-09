package eu.isygoit.dto.response;

/**
 * An optional task that was not selected.
 *
 * @param taskId       the task
 * @param reasonCode   NO_ELIGIBLE_RESOURCE | PREDECESSOR_NOT_SELECTED | OUTSIDE_WINDOW | NOT_SELECTED
 * @param reason       human text (English; the UI localises by reasonCode)
 * @param suggestion   what could change the outcome
 * @param forgonePrize effective prize of the task
 */
public record UnassignedTask(
        String taskId,
        String reasonCode,
        String reason,
        String suggestion,
        long forgonePrize) {
}
