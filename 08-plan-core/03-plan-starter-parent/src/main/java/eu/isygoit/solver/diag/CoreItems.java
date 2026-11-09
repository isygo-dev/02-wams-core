package eu.isygoit.solver.diag;

import eu.isygoit.dto.response.CoreItem;
import eu.isygoit.dto.response.Diagnostics;
import eu.isygoit.dto.response.DiagnosticIssue;
import eu.isygoit.dto.response.InfeasibilityCore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Vocabulary and helpers shared by the infeasibility explanation phases (design 3, 6.10, 8.3): core item kinds, reason
 * codes, methods, and the deterministic construction of {@link InfeasibilityCore} / {@link Diagnostics}.
 * <p>
 * Items are de-duplicated on (kind, id, reasonCode) and ordered by kind ({@code CONSTRAINT, TASK, RESOURCE, EDGE}) then
 * id, which is the order promised by design 8.3. Stateless utility; thread-safe.
 */
public final class CoreItems {

    // ---- kinds
    /** A catalogue constraint is part of the core. */
    public static final String CONSTRAINT = "CONSTRAINT";
    /** A task (its window / pin / lock / candidates) is part of the core. */
    public static final String TASK = "TASK";
    /** A resource (calendar / load cap) is part of the core. */
    public static final String RESOURCE = "RESOURCE";
    /** A precedence edge, id {@code predecessor->successor}. */
    public static final String EDGE = "EDGE";

    // ---- reason codes
    public static final String NO_ELIGIBLE_RESOURCE = "NO_ELIGIBLE_RESOURCE";
    public static final String WINDOW_EMPTY = "WINDOW_EMPTY";
    public static final String CRITICAL_PATH_TOO_LONG = "CRITICAL_PATH_TOO_LONG";
    public static final String CAPACITY_EXCEEDED = "CAPACITY_EXCEEDED";
    public static final String PRECEDENCE = "PRECEDENCE";
    public static final String CALENDAR = "CALENDAR";
    public static final String REQUIREMENT_COUNT = "REQUIREMENT_COUNT";
    public static final String PIN_CONFLICT = "PIN_CONFLICT";
    public static final String LOCKED_CONFLICT = "LOCKED_CONFLICT";
    public static final String CONSTRAINT_REASON = "CONSTRAINT";
    public static final String HORIZON = "HORIZON";

    // ---- methods
    public static final String METHOD_PRESOLVE = "PRESOLVE_ANALYSIS";
    public static final String METHOD_ASSUMPTIONS = "ASSUMPTIONS";
    public static final String METHOD_ASSUMPTIONS_SHRUNK = "ASSUMPTIONS_SHRUNK";
    public static final String METHOD_NONE = "NONE";

    private CoreItems() {
    }

    /** @return a new item */
    public static CoreItem item(String kind, String id, String reasonCode, String message) {
        return new CoreItem(kind, id, reasonCode, message);
    }

    /** @return an EDGE item with id {@code predecessor->successor} */
    public static CoreItem edge(String predecessorId, String successorId, String message) {
        return new CoreItem(EDGE, predecessorId + "->" + successorId, PRECEDENCE, message);
    }

    /**
     * De-duplicates on (kind, id, reasonCode) keeping the first message, then sorts by kind order and id.
     *
     * @param items any collection
     * @return the ordered, unmodifiable list
     */
    public static List<CoreItem> ordered(Collection<CoreItem> items) {
        Map<String, CoreItem> unique = new LinkedHashMap<>();
        for (CoreItem item : items) {
            unique.putIfAbsent(item.kind() + '\u0000' + item.id() + '\u0000' + item.reasonCode(), item);
        }
        List<CoreItem> out = new ArrayList<>(unique.values());
        out.sort(Comparator.comparingInt((CoreItem i) -> kindRank(i.kind()))
                .thenComparing(CoreItem::id)
                .thenComparing(i -> i.reasonCode() == null ? "" : i.reasonCode()));
        return List.copyOf(out);
    }

    /**
     * @param method  one of the {@code METHOD_*} constants
     * @param minimal whether the core is minimal
     * @param items   the items (de-duplicated and ordered here)
     * @return the structured core
     */
    public static InfeasibilityCore core(String method, boolean minimal, Collection<CoreItem> items) {
        return new InfeasibilityCore(minimal, method, ordered(items));
    }

    /**
     * Legacy flat list of ids kept for CSV/UI (design 3): constraint ids and task ids, distinct, in item order.
     *
     * @param core the structured core (may be {@code null})
     * @return the flat ids, never {@code null}
     */
    public static List<String> flatIds(InfeasibilityCore core) {
        if (core == null) {
            return List.of();
        }
        Set<String> ids = new LinkedHashSet<>();
        for (CoreItem item : core.items()) {
            if (CONSTRAINT.equals(item.kind()) || TASK.equals(item.kind())) {
                ids.add(item.id());
            }
        }
        return List.copyOf(ids);
    }

    /**
     * Builds the infeasibility {@link Diagnostics} for a core.
     *
     * @param message  human readable summary
     * @param core     the structured core
     * @param warnings warnings (may be {@code null})
     * @param details  extra details (may be {@code null}); copied into a {@link LinkedHashMap}
     * @return the diagnostics with the legacy flat list filled
     */
    public static Diagnostics diagnostics(String message, InfeasibilityCore core, List<DiagnosticIssue> warnings,
                                          Map<String, Object> details) {
        return new Diagnostics(message, flatIds(core), core, warnings,
                details == null ? new LinkedHashMap<>() : new LinkedHashMap<>(details));
    }

    private static int kindRank(String kind) {
        if (CONSTRAINT.equals(kind)) {
            return 0;
        }
        if (TASK.equals(kind)) {
            return 1;
        }
        if (RESOURCE.equals(kind)) {
            return 2;
        }
        if (EDGE.equals(kind)) {
            return 3;
        }
        return 4;
    }
}
