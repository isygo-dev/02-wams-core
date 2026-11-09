package eu.isygoit.solver.cpsat;

import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.LinearArgument;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.Literal;
import eu.isygoit.dto.request.PrecedenceRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Emits one precedence edge between two tasks (design 6.4, "Precedence edges"), shared by the structural edges
 * ({@code Task.predecessors}, built by {@link CpModelBuilder}) and by the {@code precedence} constraint family.
 * <p>
 * For the edge {@code i -> j} (i = predecessor) the emitted linear constraints are:
 * <pre>
 *   FINISH_START : start(j) >= end(i)   + minLag        (maxLag: start(j) <= end(i)   + maxLag)
 *   START_START  : start(j) >= start(i) + minLag        (maxLag: start(j) <= start(i) + maxLag)
 *   FINISH_FINISH: end(j)   >= end(i)   + minLag        (maxLag: end(j)   <= end(i)   + maxLag)
 *   START_FINISH : end(j)   >= start(i) + minLag        (maxLag: end(j)   <= start(i) + maxLag)
 * </pre>
 * enforced by the given condition literals plus the presence literals of the OPTIONAL endpoints (constants are skipped).
 * Both sides are affine expressions: no auxiliary variable is created. Presence {@code IMPLIES} adds
 * {@code presence(j) => presence(i)} (enforced by the condition literals only).
 * <p>
 * Stateless utility; building thread only. Units: ticks.
 */
public final class PrecedenceEmitter {

    private PrecedenceEmitter() {
    }

    /**
     * Emits an edge with an optional upper lag.
     *
     * @param ctx    the model context
     * @param i      predecessor task index
     * @param j      successor task index
     * @param type   the edge type (null = FINISH_START)
     * @param minLag minimum lag in ticks (may be negative)
     * @param maxLag maximum lag in ticks, or {@code null} for none
     * @param cond   enforcement literals (for example {@code enforcer.conditions(...)}); null or empty = unconditional
     */
    public static void emit(CpModelContext ctx, int i, int j, PrecedenceRef.Type type, int minLag, Integer maxLag,
                            Literal[] cond) {
        emit(ctx, i, j, type, minLag, maxLag, PrecedenceRef.Presence.WHEN_BOTH, cond);
    }

    /**
     * Emits an edge whose upper lag is given as an {@code int} where {@link Integer#MAX_VALUE} means "no upper lag" (the
     * encoding of {@code CandidateIndex.edgeMaxLag}).
     *
     * @param ctx    the model context
     * @param i      predecessor task index
     * @param j      successor task index
     * @param type   the edge type (null = FINISH_START)
     * @param minLag minimum lag in ticks
     * @param maxLag maximum lag in ticks or {@link Integer#MAX_VALUE} for none
     * @param cond   enforcement literals; null or empty = unconditional
     */
    public static void emit(CpModelContext ctx, int i, int j, PrecedenceRef.Type type, int minLag, int maxLag,
                            Literal[] cond) {
        emit(ctx, i, j, type, minLag, maxLag == Integer.MAX_VALUE ? null : Integer.valueOf(maxLag),
                PrecedenceRef.Presence.WHEN_BOTH, cond);
    }

    /**
     * Full form of the emission.
     *
     * @param ctx      the model context
     * @param i        predecessor task index
     * @param j        successor task index
     * @param type     the edge type (null = FINISH_START)
     * @param minLag   minimum lag in ticks
     * @param maxLag   maximum lag in ticks or {@code null}
     * @param presence WHEN_BOTH (default) or IMPLIES (successor present implies predecessor present)
     * @param cond     enforcement literals; null or empty = unconditional
     */
    public static void emit(CpModelContext ctx, int i, int j, PrecedenceRef.Type type, int minLag, Integer maxLag,
                            PrecedenceRef.Presence presence, Literal[] cond) {
        if (i == j) {
            return;
        }
        CpModel model = ctx.model();
        PrecedenceRef.Type effective = type == null ? PrecedenceRef.Type.FINISH_START : type;
        LinearArgument predecessorPoint = switch (effective) {
            case FINISH_START, FINISH_FINISH -> ctx.end(i);
            case START_START, START_FINISH -> ctx.start(i);
        };
        LinearArgument successorPoint = switch (effective) {
            case FINISH_START, START_START -> ctx.start(j);
            case FINISH_FINISH, START_FINISH -> ctx.end(j);
        };

        List<Literal> literals = new ArrayList<>();
        if (cond != null) {
            for (Literal literal : cond) {
                if (literal != null) {
                    literals.add(literal);
                }
            }
        }
        Literal pi = ctx.presence(i);
        Literal pj = ctx.presence(j);
        if (pi != ctx.trueLit() && !literals.contains(pi)) {
            literals.add(pi);
        }
        if (pj != ctx.trueLit() && !literals.contains(pj)) {
            literals.add(pj);
        }
        Literal[] enforcement = literals.toArray(new Literal[0]);

        CpModelContext.enforce(model.addGreaterOrEqual(successorPoint, plus(predecessorPoint, minLag)), enforcement);
        int constraints = 1;
        if (maxLag != null) {
            CpModelContext.enforce(model.addLessOrEqual(successorPoint, plus(predecessorPoint, maxLag)), enforcement);
            constraints++;
        }
        if (presence == PrecedenceRef.Presence.IMPLIES && pj != ctx.trueLit()) {
            Literal[] only = cond == null ? new Literal[0] : stripNulls(cond);
            CpModelContext.enforce(model.addImplication(pj, pi), only);
            constraints++;
        } else if (presence == PrecedenceRef.Presence.IMPLIES && pi != ctx.trueLit()) {
            // successor is required: the predecessor must be present as well
            Literal[] only = cond == null ? new Literal[0] : stripNulls(cond);
            CpModelContext.enforce(model.addImplication(ctx.trueLit(), pi), only);
            constraints++;
        }
        ctx.count(0, 0, constraints, 0);
    }

    private static LinearExpr plus(LinearArgument expression, long constant) {
        return LinearExpr.newBuilder().add(expression).add(constant).build();
    }

    private static Literal[] stripNulls(Literal[] literals) {
        List<Literal> result = new ArrayList<>(literals.length);
        for (Literal literal : literals) {
            if (literal != null) {
                result.add(literal);
            }
        }
        return result.toArray(new Literal[0]);
    }
}
