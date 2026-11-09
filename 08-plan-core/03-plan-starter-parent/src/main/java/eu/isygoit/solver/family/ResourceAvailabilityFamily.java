package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.TimeWindow;
import eu.isygoit.dto.request.params.ResourceAvailabilityParams;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.CheckContext;
import eu.isygoit.solver.spi.ConstraintChecker;
import eu.isygoit.solver.spi.ConstraintFamily;
import eu.isygoit.solver.spi.ModelCost;
import eu.isygoit.solver.spi.PreprocessContext;
import eu.isygoit.solver.spi.ScopeKind;
import eu.isygoit.solver.spi.SolutionView;
import eu.isygoit.solver.spi.ValidationContext;
import eu.isygoit.solver.spi.ViolationSink;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Family {@code resource_availability} (design 4.3 / 6.5.6): extra unavailable intervals ({@code unavailable}) and/or
 * "available only" windows ({@code availableOnly}) for the scoped resources.
 * <p>
 * Blocked time = the union of {@code unavailable} and of the complement (inside the horizon) of the merged
 * {@code availableOnly} windows. A task with positive duration on a scoped resource must not overlap blocked time
 * (half-open intervals; zero-duration tasks occupy nothing). SOFT unit (overtime): one assigned (task, resource) pair
 * that overlaps blocked time (measure 1), however many blocked windows it touches.
 * <p>
 * HARD is fully handled by {@link #preprocess} (per-candidate start runs minus the blocked windows). Stateless and
 * thread-safe; scope = resources; units are ticks.
 */
@Component
public class ResourceAvailabilityFamily implements ConstraintFamily {

    /** The family id. */
    public static final String TYPE = "resource_availability";

    private static final Set<IEnumEnforcement.Types> SUPPORTED =
            Collections.unmodifiableSet(EnumSet.allOf(IEnumEnforcement.Types.class));
    private static final ConstraintChecker CHECKER = new Checker();

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<?> paramsType() {
        return ResourceAvailabilityParams.class;
    }

    @Override
    public Set<IEnumEnforcement.Types> supportedEnforcements() {
        return SUPPORTED;
    }

    @Override
    public String descriptionKey() {
        return "plan.constraint.resource_availability.description";
    }

    @Override
    public List<String> paramsHint() {
        return List.of("unavailable", "availableOnly");
    }

    @Override
    public ScopeKind scopeKind() {
        return ScopeKind.RESOURCES;
    }

    @Override
    public void validate(ValidationContext ctx, Constraint constraint, Object params) {
        if (!(params instanceof ResourceAvailabilityParams p)) {
            return;
        }
        if (p.unavailable().isEmpty() && p.availableOnly().isEmpty()) {
            ctx.error("PARAMS_INVALID", path(ctx, constraint, null), "resource_availability needs unavailable and/or availableOnly windows");
            return;
        }
        validateWindows(ctx, constraint, "unavailable", p.unavailable());
        validateWindows(ctx, constraint, "availableOnly", p.availableOnly());
    }

    @Override
    public ModelCost estimate(CandidateIndexView index, Constraint constraint, Object params) {
        if (!(params instanceof ResourceAvailabilityParams p)
                || constraint.enforcement() == IEnumEnforcement.Types.STRICTLY_REQUIRED
                || constraint.enforcement() == IEnumEnforcement.Types.CONFIGURABLE_HARD) {
            return ModelCost.ZERO; // HARD: preprocess prune
        }
        boolean[] inScope = scopeMask(index, constraint);
        int[] blocked = blockedRuns(index.horizon(), p);
        long windows = blocked.length / 2;
        long candidates = 0;
        for (int t = 0; t < index.taskCount(); t++) {
            for (int c = index.candidateBegin(t); c < index.candidateEnd(t); c++) {
                if (inScope[index.candidateResource(c)] && index.candidateDuration(c) > 0) {
                    candidates++;
                }
            }
        }
        long soft = constraint.enforcement() == IEnumEnforcement.Types.SOFT ? candidates : 0;
        // per candidate: <= 2 literals and <= 3 constraints per reachable window (upper bound: every window)
        long perCandidate = Math.max(1, windows);
        return new ModelCost(soft + 2 * candidates * perCandidate, 0, 3 * candidates * perCandidate, 0, 0, 0);
    }

    @Override
    public void preprocess(PreprocessContext ctx, Constraint constraint, Object params) {
        if (!(params instanceof ResourceAvailabilityParams p)) {
            return;
        }
        PlanningProblem problem = ctx.candidates().problem(); // needs from WP2: Mutable.problem()
        boolean[] inScope = new boolean[problem.resources().size()];
        for (int r : ctx.resolveResources(constraint.scope())) {
            inScope[r] = true;
        }
        int horizonTicks = (int) ctx.horizon().horizonTicks();
        int[] blocked = blockedRuns(ctx.horizon(), p);
        if (blocked.length == 0) {
            return;
        }
        Map<Integer, int[]> runsByDuration = new HashMap<>();
        for (int t = 0; t < problem.tasks().size(); t++) {
            // needs from WP2: Mutable.candidateBegin/candidateEnd/candidateResource/candidateDuration
            for (int c = ctx.candidates().candidateBegin(t); c < ctx.candidates().candidateEnd(t); c++) {
                int d = ctx.candidates().candidateDuration(c);
                if (d <= 0 || !inScope[ctx.candidates().candidateResource(c)]) {
                    continue;
                }
                int[] runs = runsByDuration.computeIfAbsent(d, k -> allowedStartRuns(blocked, k, horizonTicks));
                if (runs.length == 0) {
                    ctx.pruneCandidate(c, "UNAVAILABLE", constraint.id());
                } else {
                    // needs from WP2: Mutable.restrictCandidateStart(candidate, inclusive runs): intersects the candidate start domain
                    ctx.candidates().restrictCandidateStart(c, runs);
                }
            }
        }
    }

    @Override
    public ConstraintChecker checker() {
        return CHECKER;
    }

    // ------------------------------------------------------------------ shared semantics

    /**
     * Blocked time of the constraint in ticks.
     *
     * @return sorted, merged, half-open runs {@code [lo0,hi0,lo1,hi1,...)} clipped to the horizon; empty when nothing is blocked
     */
    public static int[] blockedRuns(Horizon h, ResourceAvailabilityParams p) {
        int horizonTicks = (int) h.horizonTicks();
        List<int[]> blocked = new ArrayList<>();
        for (TimeWindow w : p.unavailable()) {
            int lo = clamp(h.floorTick(w.start()), horizonTicks); // unavailability only ever grows
            int hi = clamp(h.ceilTick(w.end()), horizonTicks);
            if (lo < hi) {
                blocked.add(new int[]{lo, hi});
            }
        }
        if (!p.availableOnly().isEmpty()) {
            List<int[]> available = new ArrayList<>();
            for (TimeWindow w : p.availableOnly()) {
                int lo = clamp(h.ceilTick(w.start()), horizonTicks); // availability only ever shrinks
                int hi = clamp(h.floorTick(w.end()), horizonTicks);
                if (lo < hi) {
                    available.add(new int[]{lo, hi});
                }
            }
            int[] merged = merge(available);
            int cursor = 0;
            for (int k = 0; k < merged.length; k += 2) {
                if (merged[k] > cursor) {
                    blocked.add(new int[]{cursor, merged[k]});
                }
                cursor = merged[k + 1];
            }
            if (cursor < horizonTicks) {
                blocked.add(new int[]{cursor, horizonTicks});
            }
        }
        return merge(blocked);
    }

    /**
     * Forbidden START ticks of a task of duration {@code d >= 1}: the starts whose interval {@code [s, s+d)} overlaps a
     * blocked run, i.e. {@code [lo-d+1, hi-1]} per run, merged, lower bound clamped to 0.
     *
     * @return inclusive runs {@code [a0,b0,a1,b1,...]}, ascending, disjoint and non-adjacent
     */
    public static int[] forbiddenStartRuns(int[] blocked, int d) {
        List<int[]> runs = new ArrayList<>();
        for (int k = 0; k < blocked.length; k += 2) {
            int a = Math.max(0, blocked[k] - d + 1);
            int b = blocked[k + 1] - 1;
            if (a <= b) {
                runs.add(new int[]{a, b + 1}); // half-open for merging
            }
        }
        int[] merged = merge(runs);
        int[] inclusive = new int[merged.length];
        for (int k = 0; k < merged.length; k += 2) {
            inclusive[k] = merged[k];
            inclusive[k + 1] = merged[k + 1] - 1;
        }
        return inclusive;
    }

    /**
     * Allowed START ticks of a task of duration {@code d >= 1} inside {@code [0, horizonTicks - d]}.
     *
     * @return inclusive runs {@code [a0,b0,...]}; empty when no start is allowed
     */
    public static int[] allowedStartRuns(int[] blocked, int d, int horizonTicks) {
        int upper = horizonTicks - d;
        if (upper < 0) {
            return new int[0];
        }
        int[] forbidden = forbiddenStartRuns(blocked, d);
        List<Integer> out = new ArrayList<>();
        int cursor = 0;
        for (int k = 0; k < forbidden.length; k += 2) {
            if (forbidden[k] > cursor) {
                out.add(cursor);
                out.add(Math.min(upper, forbidden[k] - 1));
            }
            cursor = Math.max(cursor, forbidden[k + 1] + 1);
            if (cursor > upper) {
                break;
            }
        }
        if (cursor <= upper) {
            out.add(cursor);
            out.add(upper);
        }
        // the first segment may start beyond upper when the forbidden run began at 0 and is huge: filter invalid pairs
        List<Integer> clean = new ArrayList<>();
        for (int k = 0; k < out.size(); k += 2) {
            if (out.get(k) <= out.get(k + 1)) {
                clean.add(out.get(k));
                clean.add(out.get(k + 1));
            }
        }
        int[] result = new int[clean.size()];
        for (int k = 0; k < result.length; k++) {
            result[k] = clean.get(k);
        }
        return result;
    }

    /** @return {@code mask[r]} = the resource is selected by the constraint scope */
    public static boolean[] scopeMask(CandidateIndexView index, Constraint constraint) {
        boolean[] mask = new boolean[index.resourceCount()];
        for (int r : index.resolveResources(constraint.scope())) {
            mask[r] = true;
        }
        return mask;
    }

    private static int[] merge(List<int[]> intervals) {
        intervals.sort(Comparator.comparingInt((int[] a) -> a[0]).thenComparingInt(a -> a[1]));
        List<Integer> out = new ArrayList<>();
        for (int[] iv : intervals) {
            int n = out.size();
            if (n > 0 && iv[0] <= out.get(n - 1)) {
                out.set(n - 1, Math.max(out.get(n - 1), iv[1]));
            } else {
                out.add(iv[0]);
                out.add(iv[1]);
            }
        }
        int[] result = new int[out.size()];
        for (int k = 0; k < result.length; k++) {
            result[k] = out.get(k);
        }
        return result;
    }

    private static int clamp(int tick, int horizonTicks) {
        return Math.max(0, Math.min(horizonTicks, tick));
    }

    private static void validateWindows(ValidationContext ctx, Constraint constraint, String field, List<TimeWindow> windows) {
        Horizon h = ctx.problem().horizon();
        for (int k = 0; k < windows.size(); k++) {
            TimeWindow w = windows.get(k);
            String base = field + "[" + k + "]";
            if (w == null || w.start() == null || w.end() == null) {
                ctx.error("PARAMS_INVALID", path(ctx, constraint, base), "window start and end are required");
                continue;
            }
            if (!w.start().isBefore(w.end())) {
                ctx.error("PARAMS_INVALID", path(ctx, constraint, base), "window start must be before its end");
                continue;
            }
            if (!h.isAligned(w.start())) {
                ctx.error("TIME_NOT_ALIGNED", path(ctx, constraint, base + ".start"), "window start is not on a tick boundary");
            }
            if (!h.isAligned(w.end())) {
                ctx.error("TIME_NOT_ALIGNED", path(ctx, constraint, base + ".end"), "window end is not on a tick boundary");
            }
            if (w.start().isBefore(h.start()) || w.end().isAfter(h.end())) {
                ctx.warning("OUT_OF_HORIZON_CLIPPED", path(ctx, constraint, base), "window is clipped to the horizon");
            }
        }
    }

    private static String path(ValidationContext ctx, Constraint constraint, String field) {
        int i = ctx.problem().constraints().indexOf(constraint);
        String base = "constraints[" + (i < 0 ? "?" : String.valueOf(i)) + "].params";
        return field == null ? base : base + "." + field;
    }

    // ------------------------------------------------------------------ checker

    private static final class Checker implements ConstraintChecker {
        @Override
        public void check(CheckContext ctx, Constraint constraint, Object params, ViolationSink sink) {
            if (!(params instanceof ResourceAvailabilityParams p)) {
                return;
            }
            CandidateIndexView idx = ctx.index();
            SolutionView sol = ctx.solution();
            int[] blocked = blockedRuns(ctx.problem().horizon(), p);
            if (blocked.length == 0) {
                return;
            }
            boolean[] inScope = scopeMask(idx, constraint);
            for (int t = 0; t < sol.taskCount(); t++) {
                if (!sol.selected(t) || sol.end(t) <= sol.start(t)) {
                    continue;
                }
                for (int k = 0; k < sol.resourceCount(t); k++) {
                    int r = sol.resourceAt(t, k);
                    if (inScope[r] && overlaps(blocked, sol.start(t), sol.end(t))) {
                        sink.violation(idx.taskId(t), idx.resourceId(r), "RESOURCE_UNAVAILABLE",
                                "Task " + idx.taskId(t) + " overlaps blocked time of resource " + idx.resourceId(r), 1);
                    }
                }
            }
        }

        private static boolean overlaps(int[] blocked, int start, int end) {
            for (int k = 0; k < blocked.length; k += 2) {
                if (start < blocked[k + 1] && end > blocked[k]) {
                    return true;
                }
            }
            return false;
        }
    }
}
