package eu.isygoit.solver.family;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.PrecedenceRef;
import eu.isygoit.dto.request.params.PrecedenceParams;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.CheckContext;
import eu.isygoit.solver.spi.ConstraintChecker;
import eu.isygoit.solver.spi.ConstraintFamily;
import eu.isygoit.solver.spi.ModelCost;
import eu.isygoit.solver.spi.ScopeKind;
import eu.isygoit.solver.spi.SolutionView;
import eu.isygoit.solver.spi.ValidationContext;
import eu.isygoit.solver.spi.ViolationSink;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Family {@code precedence} (design 4.3 / 6.5.4): bulk / soft / relaxable precedence between two task sets with the
 * four link types FS / SS / FF / SF, {@code minLag} and optional {@code maxLag} (the hard edges of the definition live in
 * {@code Task.predecessors}).
 * <p>
 * Pairs: {@code from} x {@code to} (ALL_PAIRS, self pairs skipped) or the i-th {@code from} with the i-th {@code to}
 * (BY_ORDER, equal sizes required). When the constraint scope is not {@code all} it additionally filters both sides.
 * A pair is only enforced when both tasks are selected. SOFT unit: one violated pair (min OR max lag broken = 1).
 * Stateless and thread-safe; units are ticks.
 */
@Component
public class PrecedenceFamily implements ConstraintFamily {

    /** The family id. */
    public static final String TYPE = "precedence";

    private static final Set<IEnumEnforcement.Types> SUPPORTED =
            Collections.unmodifiableSet(EnumSet.allOf(IEnumEnforcement.Types.class));
    private static final ConstraintChecker CHECKER = new Checker();

    /** Receiver of the (from, to) task index pairs of a precedence constraint. */
    @FunctionalInterface
    public interface PairConsumer {
        /**
         * @param from index of the predecessor
         * @param to   index of the successor
         */
        void accept(int from, int to);
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Class<?> paramsType() {
        return PrecedenceParams.class;
    }

    @Override
    public Set<IEnumEnforcement.Types> supportedEnforcements() {
        return SUPPORTED;
    }

    @Override
    public String descriptionKey() {
        return "plan.constraint.precedence.description";
    }

    @Override
    public List<String> paramsHint() {
        return List.of("from", "to", "type", "minLag", "maxLag", "pairing");
    }

    @Override
    public ScopeKind scopeKind() {
        return ScopeKind.TASKS;
    }

    @Override
    public void validate(ValidationContext ctx, Constraint constraint, Object params) {
        if (!(params instanceof PrecedenceParams p)) {
            return;
        }
        boolean sidesOk = validateSide(ctx, constraint, "from", p.from()) & validateSide(ctx, constraint, "to", p.to());
        long horizon = ctx.problem().horizon().horizonTicks();
        if (Math.abs((long) p.minLag()) > horizon) {
            ctx.error("INVALID_LAG", path(ctx, constraint, "minLag"), "minLag must be within [-horizon, horizon] ticks");
        }
        if (p.maxLag() != null) {
            if (Math.abs((long) p.maxLag()) > horizon) {
                ctx.error("INVALID_LAG", path(ctx, constraint, "maxLag"), "maxLag must be within [-horizon, horizon] ticks");
            }
            if (p.maxLag() < p.minLag()) {
                ctx.error("INVALID_LAG", path(ctx, constraint, "maxLag"), "maxLag must not be smaller than minLag");
            }
        }
        if (sidesOk && p.pairing() == PrecedenceParams.PairingMode.BY_ORDER) {
            int[] from = side(ctx.index(), constraint, p.from());
            int[] to = side(ctx.index(), constraint, p.to());
            if (from.length != to.length) {
                ctx.error("PARAMS_INVALID", path(ctx, constraint, "pairing"),
                        "BY_ORDER needs equally sized sides but from has " + from.length + " and to has " + to.length + " tasks");
            } else {
                for (int k = 0; k < from.length; k++) {
                    if (from[k] == to[k]) {
                        ctx.error("PARAMS_INVALID", path(ctx, constraint, "pairing"),
                                "BY_ORDER pairs task '" + ctx.index().taskId(from[k]) + "' with itself");
                        break;
                    }
                }
            }
        }
    }

    @Override
    public ModelCost estimate(CandidateIndexView index, Constraint constraint, Object params) {
        if (!(params instanceof PrecedenceParams p) || p.from() == null || p.to() == null) {
            return ModelCost.ZERO;
        }
        long pairs = pairCount(index, constraint, p);
        long perPair = p.maxLag() == null ? 1 : 2;
        long soft = constraint.enforcement() == IEnumEnforcement.Types.SOFT ? pairs : 0;
        return new ModelCost(soft, 0, pairs * perPair, 0, 0, pairs);
    }

    @Override
    public ConstraintChecker checker() {
        return CHECKER;
    }

    // ------------------------------------------------------------------ shared semantics

    /**
     * Effective side of the constraint: the params scope intersected with the constraint scope (unless that is
     * {@code all}).
     *
     * @return ascending task indices
     */
    public static int[] side(CandidateIndexView index, Constraint constraint, ConstraintScope paramsScope) {
        int[] resolved = index.resolveTasks(paramsScope);
        ConstraintScope filter = constraint.scope();
        if (filter == null || ConstraintScope.ALL.equals(filter.kind())) {
            return resolved;
        }
        int[] other = index.resolveTasks(filter);
        int[] out = new int[Math.min(resolved.length, other.length)];
        int a = 0;
        int b = 0;
        int n = 0;
        while (a < resolved.length && b < other.length) {
            if (resolved[a] == other[b]) {
                out[n++] = resolved[a];
                a++;
                b++;
            } else if (resolved[a] < other[b]) {
                a++;
            } else {
                b++;
            }
        }
        return java.util.Arrays.copyOf(out, n);
    }

    /** @return the number of (from, to) pairs {@link #forEachPair} enumerates */
    public static long pairCount(CandidateIndexView index, Constraint constraint, PrecedenceParams p) {
        int[] from = side(index, constraint, p.from());
        int[] to = side(index, constraint, p.to());
        if (p.pairing() == PrecedenceParams.PairingMode.BY_ORDER) {
            return Math.min(from.length, to.length);
        }
        long common = 0;
        int a = 0;
        int b = 0;
        while (a < from.length && b < to.length) {
            if (from[a] == to[b]) {
                common++;
                a++;
                b++;
            } else if (from[a] < to[b]) {
                a++;
            } else {
                b++;
            }
        }
        return (long) from.length * to.length - common;
    }

    /** Enumerates the pairs in a deterministic order (from ascending, then to ascending); self pairs are skipped. */
    public static void forEachPair(CandidateIndexView index, Constraint constraint, PrecedenceParams p, PairConsumer consumer) {
        int[] from = side(index, constraint, p.from());
        int[] to = side(index, constraint, p.to());
        if (p.pairing() == PrecedenceParams.PairingMode.BY_ORDER) {
            for (int k = 0; k < Math.min(from.length, to.length); k++) {
                if (from[k] != to[k]) {
                    consumer.accept(from[k], to[k]);
                }
            }
            return;
        }
        for (int i : from) {
            for (int j : to) {
                if (i != j) {
                    consumer.accept(i, j);
                }
            }
        }
    }

    /**
     * Exact semantics of one pair (also the CP-SAT formulation, see the contributor).
     *
     * @return {@code true} when the min lag (or the max lag, when given) is violated
     */
    public static boolean violated(PrecedenceRef.Type type, int startI, int endI, int startJ, int endJ, int minLag, Integer maxLag) {
        long lhs;
        long rhs;
        switch (type) {
            case START_START -> {
                lhs = startJ;
                rhs = startI;
            }
            case FINISH_FINISH -> {
                lhs = endJ;
                rhs = endI;
            }
            case START_FINISH -> {
                lhs = endJ;
                rhs = startI;
            }
            default -> {
                lhs = startJ;
                rhs = endI;
            }
        }
        if (lhs < rhs + minLag) {
            return true;
        }
        return maxLag != null && lhs > rhs + maxLag;
    }

    private static boolean validateSide(ValidationContext ctx, Constraint constraint, String field, ConstraintScope scope) {
        if (scope == null) {
            ctx.error("PARAMS_INVALID", path(ctx, constraint, field), field + " is required");
            return false;
        }
        String kind = scope.kind();
        if (!(ConstraintScope.ALL.equals(kind) || "task".equals(kind) || "taskType".equals(kind) || "tag".equals(kind))) {
            ctx.error("INVALID_SCOPE", path(ctx, constraint, field + ".kind"), "precedence sides accept kind all, task, taskType or tag");
            return false;
        }
        if (!ConstraintScope.ALL.equals(kind) && (scope.value() == null || scope.value().isBlank())) {
            ctx.error("INVALID_SCOPE", path(ctx, constraint, field + ".value"), field + ".value is required for kind '" + kind + "'");
            return false;
        }
        if ("task".equals(kind) && !ctx.hasTask(scope.value())) {
            ctx.error("UNKNOWN_REFERENCE", path(ctx, constraint, field + ".value"), "Unknown task id '" + scope.value() + "'");
            return false;
        }
        return true;
    }

    private static String path(ValidationContext ctx, Constraint constraint, String field) {
        int i = ctx.problem().constraints().indexOf(constraint);
        return "constraints[" + (i < 0 ? "?" : String.valueOf(i)) + "].params." + field;
    }

    // ------------------------------------------------------------------ checker

    private static final class Checker implements ConstraintChecker {
        @Override
        public void check(CheckContext ctx, Constraint constraint, Object params, ViolationSink sink) {
            if (!(params instanceof PrecedenceParams p) || p.from() == null || p.to() == null) {
                return;
            }
            CandidateIndexView idx = ctx.index();
            SolutionView sol = ctx.solution();
            forEachPair(idx, constraint, p, (i, j) -> {
                if (sol.selected(i) && sol.selected(j)
                        && violated(p.type(), sol.start(i), sol.end(i), sol.start(j), sol.end(j), p.minLag(), p.maxLag())) {
                    sink.violation(idx.taskId(j), null, "PRECEDENCE_VIOLATED",
                            "Task " + idx.taskId(j) + " violates " + p.type() + " precedence after " + idx.taskId(i), 1);
                }
            });
        }
    }
}
