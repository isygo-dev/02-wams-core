package eu.isygoit.dto.request.params;

import java.time.Instant;
import java.util.List;

/**
 * Soft preference for a task to fit entirely in one of the specified windows. When resourceId is set, the preference
 * applies only when that resource is assigned to the task.
 *
 * @param windows   acceptable half-open time windows
 * @param resourceId optional resource assignment filter
 */
public record PreferredWindowsParams(List<Window> windows, String resourceId) {

    public PreferredWindowsParams {
        windows = windows == null ? List.of() : List.copyOf(windows);
    }

    public record Window(Instant start, Instant end) {
    }
}
