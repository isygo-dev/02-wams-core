package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.TimeWindow;
import eu.isygoit.dto.request.params.ResourceCapacityParams;
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
import java.util.List;
import java.util.Set;

/**
 * Family {@code resource_capacity} (design 4.3 / 6.5.2): lowers the capacity of the scoped resources to
 * {@code min(Resource.capacity, params.capacity)}, optionally only inside {@code params.window} (default: whole horizon).
 * <p>
 * Enforcement: HARD and RELAXABLE only (no SOFT: the catalogue has no soft unit). Semantics: at every tick of the window
 * the summed amounts of the tasks running on the resource must not exceed the lowered capacity; zero-duration tasks
 * occupy nothing. The checker reports ONE violation per resource whose measure is the number of overloaded ticks.
 * <p>
 * Preprocess (HARD, whole-horizon reduction only): a candidate with positive duration whose amount exceeds the lowered
 * capacity can never be used and is pruned. Stateless and thread-safe; scope = resources; units are ticks.
 */
@Component
public class ResourceCapacityFamily implements ConstraintFamily {

    /** The family id. */
    public static final String TYPE = "resource_capacity";

    private static final Set<IEnumEnforcement.Types> SUPPORTED = Collections.unmodifiableSet(EnumSet.of(
            IEnumEnforcement.Types.STRICTLY_REQUIRED, IEnumEnforcement.Types.CONFIGURABLE_HARD,
            IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION));
    private static final ConstraintChecker CHECKER = new Checker();

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<?> paramsType() {
        return ResourceCapacityParams.class;
    }

    @Override
    public Set<IEnumEnforcement.Types> supportedEnforcements() {
        return SUPPORTED;
    }

    @Override
    public String descriptionKey() {
        return "plan.constraint.resource_capacity.description";
    }

    @Override
    public List<String> paramsHint() {
        return List.of("capacity", "window");
    }

    @Override
    public ScopeKind scopeKind() {
        return ScopeKind.RESOURCES;
    }

    @Override
    public void validate(ValidationContext ctx, Constraint constraint, Object params) {
        if (!(params instanceof ResourceCapacityParams p)) {
            return;
        }
        if (p.capacity() == null) {
            ctx.error("PARAMS_INVALID", path(ctx, constraint, "capacity"), "capacity is required");
        } else if (p.capacity() < 0) {
            ctx.error("PARAMS_INVALID", path(ctx, constraint, "capacity"), "capacity must not be negative");
        }
        TimeWindow w = p.window();
        if (w != null) {
            Horizon h = ctx.problem().horizon();
            if (w.start() == null || w.end() == null) {
                ctx.error("PARAMS_INVALID", path(ctx, constraint, "window"), "window.start and window.end are required");
            } else if (!w.start().isBefore(w.end())) {
                ctx.error("PARAMS_INVALID", path(ctx, constraint, "window"), "window.start must be before window.end");
            } else {
                if (!h.isAligned(w.start())) {
                    ctx.error("TIME_NOT_ALIGNED", path(ctx, constraint, "window.start"), "window.start is not on a tick boundary");
                }
                if (!h.isAligned(w.end())) {
                    ctx.error("TIME_NOT_ALIGNED", path(ctx, constraint, "window.end"), "window.end is not on a tick boundary");
                }
                if (w.start().isBefore(h.start()) || w.end().isAfter(h.end())) {
                    ctx.warning("OUT_OF_HORIZON_CLIPPED", path(ctx, constraint, "window"), "window is clipped to the horizon");
                }
            }
        }
    }

    @Override
    public ModelCost estimate(CandidateIndexView index, Constraint constraint, Object params) {
        if (!(params instanceof ResourceCapacityParams p) || p.capacity() == null) {
            return ModelCost.ZERO;
        }
        long blocks = 0;
        for (int r : index.resolveResources(constraint.scope())) {
            if (newCapacity(index.capacity(r), p) < index.capacity(r)) {
                blocks++;
            }
        }
        return new ModelCost(0, 0, blocks, blocks, 0, 0);
    }

    @Override
    public void preprocess(PreprocessContext ctx, Constraint constraint, Object params) {
        if (!(params instanceof ResourceCapacityParams p) || p.capacity() == null || p.window() != null) {
            return; // only a whole-horizon reduction prunes
        }
        PlanningProblem problem = ctx.candidates().problem(); // needs from WP2: Mutable.problem()
        int[] newCap = new int[problem.resources().size()];
        java.util.Arrays.fill(newCap, -1);
        for (int r : ctx.resolveResources(constraint.scope())) {
            int cap = problem.resources().get(r).capacity();
            if (newCapacity(cap, p) < cap) {
                newCap[r] = newCapacity(cap, p);
            }
        }
        for (int t = 0; t < problem.tasks().size(); t++) {
            // needs from WP2: Mutable.candidateBegin/candidateEnd/candidateResource/candidateDuration/candidateAmount
            for (int c = ctx.candidates().candidateBegin(t); c < ctx.candidates().candidateEnd(t); c++) {
                int cap = newCap[ctx.candidates().candidateResource(c)];
                if (cap >= 0 && ctx.candidates().candidateDuration(c) > 0 && ctx.candidates().candidateAmount(c) > cap) {
                    ctx.pruneCandidate(c, "CAPACITY_AMOUNT", constraint.id());
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
     * @param resourceCapacity the structural capacity of the resource
     * @param p                the params ({@code capacity} must be non-null)
     * @return {@code min(resourceCapacity, p.capacity)}
     */
    public static int newCapacity(int resourceCapacity, ResourceCapacityParams p) {
        return Math.min(resourceCapacity, p.capacity());
    }

    /**
     * @return {@code {lo, hi}} half-open tick window clipped to the horizon (whole horizon when the window is null);
     * {@code lo >= hi} means the window is empty
     */
    public static int[] windowTicks(Horizon h, ResourceCapacityParams p) {
        int horizonTicks = (int) h.horizonTicks();
        if (p.window() == null) {
            return new int[]{0, horizonTicks};
        }
        int lo = Math.max(0, Math.min(horizonTicks, h.floorTick(p.window().start())));
        int hi = Math.max(0, Math.min(horizonTicks, h.ceilTick(p.window().end())));
        return new int[]{lo, hi};
    }

    private static String path(ValidationContext ctx, Constraint constraint, String field) {
        int i = ctx.problem().constraints().indexOf(constraint);
        return "constraints[" + (i < 0 ? "?" : String.valueOf(i)) + "].params." + field;
    }

    // ------------------------------------------------------------------ checker

    private record Event(int time, int delta) {
    }

    private static final class Checker implements ConstraintChecker {
        @Override
        public void check(CheckContext ctx, Constraint constraint, Object params, ViolationSink sink) {
            if (!(params instanceof ResourceCapacityParams p) || p.capacity() == null) {
                return;
            }
            CandidateIndexView idx = ctx.index();
            SolutionView sol = ctx.solution();
            int[] window = windowTicks(ctx.problem().horizon(), p);
            if (window[0] >= window[1]) {
                return;
            }
            for (int r : idx.resolveResources(constraint.scope())) {
                int cap = newCapacity(idx.capacity(r), p);
                if (cap >= idx.capacity(r)) {
                    continue;
                }
                List<Event> events = new ArrayList<>();
                for (int t : sol.tasksOfResource(r)) {
                    int lo = Math.max(sol.start(t), window[0]);
                    int hi = Math.min(sol.end(t), window[1]);
                    if (lo >= hi) {
                        continue;
                    }
                    int amount = amountOn(sol, t, r);
                    events.add(new Event(lo, amount));
                    events.add(new Event(hi, -amount));
                }
                events.sort(Comparator.comparingInt(Event::time).thenComparingInt(Event::delta));
                long load = 0;
                long over = 0;
                int prev = window[0];
                for (Event e : events) {
                    if (load > cap) {
                        over += e.time() - prev;
                    }
                    load += e.delta();
                    prev = e.time();
                }
                if (over > 0) {
                    sink.violation(null, idx.resourceId(r), "CAPACITY_REDUCTION_EXCEEDED",
                            "Resource " + idx.resourceId(r) + " exceeds the reduced capacity " + cap + " during " + over + " ticks",
                            over);
                }
            }
        }

        private static int amountOn(SolutionView sol, int task, int resource) {
            for (int k = 0; k < sol.resourceCount(task); k++) {
                if (sol.resourceAt(task, k) == resource) {
                    return sol.amountAt(task, k);
                }
            }
            return 0;
        }
    }
}
