package eu.isygoit.solver.timefold.contrib;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.TimeWindow;
import eu.isygoit.dto.request.params.ResourceAvailabilityParams;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.timefold.SlotOptions;
import eu.isygoit.solver.timefold.TimefoldBuildContext;
import eu.isygoit.solver.timefold.TimefoldConstraintContributor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Timefold formulation of {@code resource_availability} (design 4.3): extra unavailable intervals and/or "available
 * only" windows for the scoped resources, as NEUTRAL pruning of options:
 * <ul>
 *   <li>an option whose interval {@code [start, end)} overlaps an {@code unavailable} window is forbidden (the blocked
 *       window grows on the tick grid: start floored, end ceiled);</li>
 *   <li>when {@code availableOnly} is not empty, an option must lie inside ONE merged window (the available window
 *       shrinks on the tick grid: start ceiled, end floored).</li>
 * </ul>
 * Idempotent with the family preprocess (a constraint with {@code active == false} is skipped). HARD only. Stateless
 * Spring bean, thread-safe; runs on the building thread. Scope: RESOURCES.
 */
@Component
public class ResourceAvailabilityTimefoldContributor implements TimefoldConstraintContributor {

    /** The family id. */
    public static final String TYPE = "resource_availability";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public EnforcementSupport support() {
        return EnforcementSupport.HARD_ONLY;
    }

    @Override
    public void contribute(TimefoldBuildContext ctx, CompiledConstraint compiled) {
        if (!compiled.active() || !(compiled.params() instanceof ResourceAvailabilityParams params)) {
            return;
        }
        Constraint constraint = compiled.constraint();
        Horizon horizon = ctx.index().horizon();
        List<long[]> blocked = new ArrayList<>();
        for (TimeWindow w : params.unavailable()) {
            if (w != null && w.start() != null && w.end() != null) {
                blocked.add(new long[]{horizon.floorTick(w.start()), horizon.ceilTick(w.end())});
            }
        }
        List<long[]> available = new ArrayList<>();
        for (TimeWindow w : params.availableOnly()) {
            if (w != null && w.start() != null && w.end() != null) {
                available.add(new long[]{horizon.ceilTick(w.start()), horizon.floorTick(w.end())});
            }
        }
        long[][] merged = merge(available);
        boolean restrictToWindows = !params.availableOnly().isEmpty();
        boolean[] resourceMask = ctx.resourceMask(constraint.scope());
        SlotOptions options = ctx.options();
        for (int o = 0; o < options.optionCount(); o++) {
            if (resourceMask != null && !resourceMask[options.resource(o)]) {
                continue;
            }
            long start = options.start(o);
            long end = options.end(o);
            if (overlapsAny(blocked, start, end) || (restrictToWindows && !insideOne(merged, start, end))) {
                ctx.forbid(o, constraint.id());
            }
        }
    }

    private static boolean overlapsAny(List<long[]> blocked, long start, long end) {
        for (long[] b : blocked) {
            if (start < b[1] && b[0] < end) {
                return true;
            }
        }
        return false;
    }

    private static boolean insideOne(long[][] windows, long start, long end) {
        for (long[] w : windows) {
            if (start >= w[0] && end <= w[1]) {
                return true;
            }
        }
        return false;
    }

    /** Sorts by start and merges overlapping or touching windows; empty windows are dropped. */
    private static long[][] merge(List<long[]> windows) {
        List<long[]> sorted = new ArrayList<>();
        for (long[] w : windows) {
            if (w[0] < w[1]) {
                sorted.add(w);
            }
        }
        sorted.sort((a, b) -> Long.compare(a[0], b[0]));
        List<long[]> result = new ArrayList<>();
        for (long[] w : sorted) {
            if (!result.isEmpty() && w[0] <= result.get(result.size() - 1)[1]) {
                long[] last = result.get(result.size() - 1);
                last[1] = Math.max(last[1], w[1]);
            } else {
                result.add(new long[]{w[0], w[1]});
            }
        }
        return result.toArray(new long[0][]);
    }
}
