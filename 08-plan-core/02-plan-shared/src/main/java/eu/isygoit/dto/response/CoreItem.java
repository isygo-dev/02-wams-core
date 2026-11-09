package eu.isygoit.dto.response;

/**
 * One element of an infeasibility core.
 *
 * @param kind       CONSTRAINT | TASK | RESOURCE | EDGE
 * @param id         id of the constraint / task / resource, or an edge label
 * @param reasonCode NO_ELIGIBLE_RESOURCE, WINDOW_EMPTY, CRITICAL_PATH_TOO_LONG, CAPACITY_EXCEEDED, PRECEDENCE,
 *                   CALENDAR, REQUIREMENT_COUNT, PIN_CONFLICT, LOCKED_CONFLICT, CONSTRAINT (a catalogue constraint
 *                   is part of the core), HORIZON
 * @param message    human readable detail
 */
public record CoreItem(String kind, String id, String reasonCode, String message) {
}
