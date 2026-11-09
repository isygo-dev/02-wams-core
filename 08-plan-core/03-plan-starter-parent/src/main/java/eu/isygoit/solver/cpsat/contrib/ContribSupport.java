package eu.isygoit.solver.cpsat.contrib;

import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.Literal;
import eu.isygoit.exception.LimitExceededException;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.index.CandidateIndex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Small helpers shared by the WP4b CP-SAT contributors and the setup circuit module: conditional enforcement, candidate
 * literals grouped by resource, role lookup, size accounting and the pairwise guard.
 * <p>
 * Stateless, thread-safe, no OR-Tools state of its own (every method works on the {@link CpModelContext} it is given, which
 * is single-threaded per build). Units: ticks.
 */
public final class ContribSupport {

    private ContribSupport() {
    }

    /**
     * Makes a model constraint conditional on the given literals; {@code null} literals are skipped and an empty list leaves
     * the constraint unconditional (the HARD solve pass).
     *
     * @param constraint the OR-Tools constraint just created
     * @param literals   enforcement literals, may contain null
     */
    public static void enforce(com.google.ortools.sat.Constraint constraint, Literal... literals) {
        List<Literal> actual = new ArrayList<>(literals.length);
        for (Literal l : literals) {
            if (l != null) {
                actual.add(l);
            }
        }
        if (actual.isEmpty()) {
            return;
        }
        if (actual.size() == 1) {
            constraint.onlyEnforceIf(actual.get(0));
        } else {
            constraint.onlyEnforceIf(actual.toArray(new Literal[0]));
        }
    }

    /** @return the affine expression {@code start(t) + offset} (the end of a candidate with that duration) */
    public static LinearExpr endOf(CpModelContext ctx, int task, int offset) {
        return LinearExpr.affine(ctx.start(task), 1, offset);
    }

    /** Clamped {@link CpModelContext#count} (size guard against the configured limits). */
    public static void count(CpModelContext ctx, long boolVars, long intVars, long constraints, long intervals) {
        ctx.count(clamp(boolVars), clamp(intVars), clamp(constraints), clamp(intervals));
    }

    private static int clamp(long v) {
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, v));
    }

    /**
     * Pairwise guard of the Universal contributor rule: in the solve pass exceeding {@code max-pairwise} is an error, in the
     * explain pass the family is skipped (the caller returns).
     *
     * @param ctx   model context
     * @param pairs number of pair literals that would be created
     * @return true when the family may build its pairs, false when it must be skipped (explain pass only)
     * @throws LimitExceededException in the solve pass when the limit is exceeded
     */
    public static boolean pairwiseOk(CpModelContext ctx, long pairs) {
        long limit = ctx.index().limits().getMaxPairwise();
        if (pairs <= limit) {
            return true;
        }
        if (ctx.explainMode()) {
            return false;
        }
        throw new LimitExceededException("max-pairwise", pairs, limit);
    }

    /**
     * @param index index
     * @param cand  candidate id
     * @return the role of the requirement the candidate belongs to
     */
    public static String roleOf(CandidateIndex index, int cand) {
        return index.roles[index.reqRole[index.candReq[cand]]];
    }

    /**
     * Candidate literals of a task grouped by resource.
     *
     * @param ctx   model context
     * @param index index
     * @param task  task index
     * @param role  role filter, null = every requirement
     * @return resource index to the candidate literals (usually one) of the task on it, ascending resource
     */
    public static Map<Integer, List<Literal>> literalsByResource(CpModelContext ctx, CandidateIndex index, int task, String role) {
        Map<Integer, List<Literal>> out = new TreeMap<>();
        for (int c = index.candidateBegin(task); c < index.candidateEnd(task); c++) {
            if (role != null && !role.equals(roleOf(index, c))) {
                continue;
            }
            out.computeIfAbsent(index.candidateResource(c), k -> new ArrayList<>(1)).add(ctx.cand(c));
        }
        return out;
    }
}
