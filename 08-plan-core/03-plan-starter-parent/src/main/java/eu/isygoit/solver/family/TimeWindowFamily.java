package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.params.TimeWindowParams;
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

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Family {@code time_window} (design 4.3 / 6.5.5): extra windows on the scoped tasks: earliest / latest START and
 * earliest / latest END (any subset, at least one).
 * <p>
 * Semantics for a selected task: {@code start >= earliestStart}, {@code start <= latestStart},
 * {@code end >= earliestEnd}, {@code end <= latestEnd} (end = start + duration actually used). SOFT unit: the number of
 * ticks outside the window, summed over the four bounds; the checker reports one violation per violated bound whose
 * measure is the number of ticks, so the total equals the model measure.
 * <p>
 * Preprocess (HARD): the start domain is restricted ({@code restrictStart}); end bounds are converted with the weakest
 * candidate duration, and the CP-SAT contributor adds the exact end constraint for tasks whose candidate durations differ.
 * Stateless and thread-safe; scope = tasks; units are ticks.
 */
@Component
public class TimeWindowFamily implements ConstraintFamily {

    /** The family id. */
    public static final String TYPE = "time_window";
    /** Marker of an absent bound in {@link #bounds}. */
    public static final int NONE = Integer.MIN_VALUE;
    /** Index of the earliest-start bound in {@link #bounds}. */
    public static final int EARLIEST_START = 0;
    /** Index of the latest-start bound in {@link #bounds}. */
    public static final int LATEST_START = 1;
    /** Index of the earliest-end bound in {@link #bounds}. */
    public static final int EARLIEST_END = 2;
    /** Index of the latest-end bound in {@link #bounds}. */
    public static final int LATEST_END = 3;

    private static final Set<IEnumEnforcement.Types> SUPPORTED =
            Collections.unmodifiableSet(EnumSet.allOf(IEnumEnforcement.Types.class));
    private static final ConstraintChecker CHECKER = new Checker();

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<?> paramsType() {
        return TimeWindowParams.class;
    }

    @Override
    public Set<IEnumEnforcement.Types> supportedEnforcements() {
        return SUPPORTED;
    }

    @Override
    public String descriptionKey() {
        return "plan.constraint.time_window.description";
    }

    @Override
    public List<String> paramsHint() {
        return List.of("earliestStart", "latestStart", "earliestEnd", "latestEnd");
    }

    @Override
    public ScopeKind scopeKind() {
        return ScopeKind.TASKS;
    }

    @Override
    public void validate(ValidationContext ctx, Constraint constraint, Object params) {
        if (!(params instanceof TimeWindowParams p)) {
            return;
        }
        if (p.earliestStart() == null && p.latestStart() == null && p.earliestEnd() == null && p.latestEnd() == null) {
            ctx.error("PARAMS_INVALID", path(ctx, constraint, null),
                    "time_window needs at least one of earliestStart, latestStart, earliestEnd, latestEnd");
            return;
        }
        int es = tick(ctx, constraint, "earliestStart", p.earliestStart());
        int ls = tick(ctx, constraint, "latestStart", p.latestStart());
        int ee = tick(ctx, constraint, "earliestEnd", p.earliestEnd());
        int le = tick(ctx, constraint, "latestEnd", p.latestEnd());
        if (es >= 0 && ls >= 0 && es > ls) {
            ctx.error("PARAMS_INVALID", path(ctx, constraint, "latestStart"), "latestStart must not be before earliestStart");
        }
        if (ee >= 0 && le >= 0 && ee > le) {
            ctx.error("PARAMS_INVALID", path(ctx, constraint, "latestEnd"), "latestEnd must not be before earliestEnd");
        }
    }

    @Override
    public ModelCost estimate(CandidateIndexView index, Constraint constraint, Object params) {
        if (!(params instanceof TimeWindowParams p)) {
            return ModelCost.ZERO;
        }
        if (constraint.enforcement() == IEnumEnforcement.Types.STRICTLY_REQUIRED
                || constraint.enforcement() == IEnumEnforcement.Types.CONFIGURABLE_HARD) {
            return ModelCost.ZERO; // preprocess prune (end constraints only for variable durations: negligible)
        }
        long bounds = (p.earliestStart() != null ? 1 : 0) + (p.latestStart() != null ? 1 : 0)
                + (p.earliestEnd() != null ? 1 : 0) + (p.latestEnd() != null ? 1 : 0);
        long instances = bounds * index.resolveTasks(constraint.scope()).length;
        boolean soft = constraint.enforcement() == IEnumEnforcement.Types.SOFT;
        return new ModelCost(0, soft ? instances : 0, instances, 0, 0, 0);
    }

    @Override
    public void preprocess(PreprocessContext ctx, Constraint constraint, Object params) {
        if (!(params instanceof TimeWindowParams p)) {
            return;
        }
        int[] b = bounds(ctx.horizon(), p);
        int horizonTicks = (int) ctx.horizon().horizonTicks();
        for (int t : ctx.resolveTasks(constraint.scope())) {
            long minDur = Long.MAX_VALUE;
            long maxDur = Long.MIN_VALUE;
            // needs from WP2: Mutable.candidateBegin/candidateEnd/candidateDuration
            for (int c = ctx.candidates().candidateBegin(t); c < ctx.candidates().candidateEnd(t); c++) {
                minDur = Math.min(minDur, ctx.candidates().candidateDuration(c));
                maxDur = Math.max(maxDur, ctx.candidates().candidateDuration(c));
            }
            long lo = 0;
            long hi = horizonTicks;
            if (b[EARLIEST_START] != NONE) {
                lo = Math.max(lo, b[EARLIEST_START]);
            }
            if (b[LATEST_START] != NONE) {
                hi = Math.min(hi, b[LATEST_START]);
            }
            if (maxDur != Long.MIN_VALUE) {
                if (b[EARLIEST_END] != NONE) {
                    lo = Math.max(lo, b[EARLIEST_END] - maxDur);
                }
                if (b[LATEST_END] != NONE) {
                    hi = Math.min(hi, b[LATEST_END] - minDur);
                }
            }
            if (lo > 0 || hi < horizonTicks) {
                ctx.restrictStart(t, (int) lo, (int) Math.max(Integer.MIN_VALUE, hi));
            }
        }
    }

    @Override
    public ConstraintChecker checker() {
        return CHECKER;
    }

    // ------------------------------------------------------------------ shared semantics

    /**
     * Converts the four bounds to ticks (clamped to {@code [0, horizonTicks]}); the instants were validated as aligned.
     *
     * @return {@code {earliestStart, latestStart, earliestEnd, latestEnd}}, {@link #NONE} for an absent bound
     */
    public static int[] bounds(Horizon h, TimeWindowParams p) {
        return new int[]{
                toTick(h, p.earliestStart()), toTick(h, p.latestStart()), toTick(h, p.earliestEnd()), toTick(h, p.latestEnd())};
    }

    private static int toTick(Horizon h, Instant t) {
        if (t == null) {
            return NONE;
        }
        return Math.max(0, Math.min((int) h.horizonTicks(), h.floorTick(t)));
    }

    /**
     * Violation (ticks outside the window) of the bound {@code which} for a task occupying {@code [start, end)}.
     *
     * @param which one of {@link #EARLIEST_START} ... {@link #LATEST_END}
     * @param bound the bound tick, not {@link #NONE}
     * @return the number of ticks violated, 0 when satisfied
     */
    public static long violation(int which, int bound, int start, int end) {
        return switch (which) {
            case EARLIEST_START -> Math.max(0L, (long) bound - start);
            case LATEST_START -> Math.max(0L, (long) start - bound);
            case EARLIEST_END -> Math.max(0L, (long) bound - end);
            default -> Math.max(0L, (long) end - bound);
        };
    }

    private static int tick(ValidationContext ctx, Constraint constraint, String field, Instant t) {
        return t == null ? -1 : ctx.tick(t, path(ctx, constraint, field));
    }

    private static String path(ValidationContext ctx, Constraint constraint, String field) {
        int i = ctx.problem().constraints().indexOf(constraint);
        String base = "constraints[" + (i < 0 ? "?" : String.valueOf(i)) + "].params";
        return field == null ? base : base + "." + field;
    }

    // ------------------------------------------------------------------ checker

    private static final String[] CODES = {"EARLIEST_START_VIOLATED", "LATEST_START_VIOLATED", "EARLIEST_END_VIOLATED",
            "LATEST_END_VIOLATED"};

    private static final class Checker implements ConstraintChecker {
        @Override
        public void check(CheckContext ctx, Constraint constraint, Object params, ViolationSink sink) {
            if (!(params instanceof TimeWindowParams p)) {
                return;
            }
            CandidateIndexView idx = ctx.index();
            SolutionView sol = ctx.solution();
            int[] b = bounds(ctx.problem().horizon(), p);
            for (int t : idx.resolveTasks(constraint.scope())) {
                if (!sol.selected(t)) {
                    continue;
                }
                for (int which = 0; which < 4; which++) {
                    if (b[which] == NONE) {
                        continue;
                    }
                    long v = violation(which, b[which], sol.start(t), sol.end(t));
                    if (v > 0) {
                        sink.violation(idx.taskId(t), null, CODES[which],
                                "Task " + idx.taskId(t) + " is " + v + " ticks outside its time window (" + CODES[which] + ")", v);
                    }
                }
            }
        }
    }
}
