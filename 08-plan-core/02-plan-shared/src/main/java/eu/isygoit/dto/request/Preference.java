package eu.isygoit.dto.request;

import java.time.Instant;

/**
 * A soft preference of a task, read by the {@code minimize_preference_penalty} objective.
 * <pre>{@code
 * { "kind": "RESOURCE", "resourceId": "teacher-alice", "penalty": 5 }
 * { "kind": "START", "start": "2026-09-07T08:00:00Z", "penalty": 1 }
 * }</pre>
 *
 * @param kind       {@link Kind#RESOURCE}: penalty if the task is NOT assigned to {@code resourceId};
 *                   {@link Kind#START}: penalty per tick of {@code |start - preferred start|}
 * @param resourceId preferred resource (RESOURCE kind)
 * @param start      preferred start instant, tick aligned (START kind)
 * @param penalty    penalty weight, {@code >= 0}
 */
public record Preference(Kind kind, String resourceId, Instant start, int penalty) {

    /**
     * Preference kinds.
     */
    public enum Kind { RESOURCE, START }
}
