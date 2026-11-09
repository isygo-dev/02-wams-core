package eu.isygoit.dto.request;

import eu.isygoit.util.CollectionCopies;

import java.util.List;

/**
 * A working calendar shared by the resources that reference it through {@code Resource.calendarId}.
 * <p>
 * Semantics: {@code availability = (windows UNION expand(recurring)) MINUS (exceptions UNION expand(breaks))}; if
 * {@code windows} AND {@code recurring} are both empty the calendar is "always available" minus exceptions and
 * breaks. A task must lie inside ONE availability window of every resource it uses (design D-5).
 *
 * @param id         unique calendar id
 * @param name       display only
 * @param timezone   IANA zone id used to expand {@code recurring}/{@code breaks}; null = {@code meta.timezone}
 * @param windows    explicit working windows (instants)
 * @param recurring  recurring working windows
 * @param exceptions explicit unavailable intervals (holidays, maintenance)
 * @param breaks     recurring unavailable intervals (lunch)
 */
public record Calendar(
        String id,
        String name,
        String timezone,
        List<TimeWindow> windows,
        List<RecurringWindow> recurring,
        List<TimeWindow> exceptions,
        List<RecurringWindow> breaks) {

    public Calendar {
        windows = CollectionCopies.list(windows);
        recurring = CollectionCopies.list(recurring);
        exceptions = CollectionCopies.list(exceptions);
        breaks = CollectionCopies.list(breaks);
    }
}
