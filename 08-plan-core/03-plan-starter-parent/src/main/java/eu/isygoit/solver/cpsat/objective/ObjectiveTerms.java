package eu.isygoit.solver.cpsat.objective;

import com.google.ortools.sat.Constraint;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearArgument;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;
import com.google.ortools.sat.Literal;
import eu.isygoit.exception.ObjectiveRangeException;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.ObjectiveSink;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.util.ScaledMath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Helpers shared by the CP-SAT objective contributors: an exact-bound term accumulator, the coefficient guard and the
 * "task is not on resource" / "start deviates from tick" building blocks of the preference and plan-change objectives.
 * <p>
 * Static, stateless; the {@link Accumulator} is used on the building thread only. OR-TOOLS-VERIFY: {@code LinearExpr.newBuilder()},
 * {@code LinearExprBuilder.add(long) / add(LinearArgument) / addTerm(LinearArgument, long) / build()} and that
 * {@code Literal}/{@code IntVar} are {@code LinearArgument}s (OR-Tools 9.15 Java).
 */
final class ObjectiveTerms {

    private ObjectiveTerms() {
    }

    /**
     * Exact coefficient {@code weight * unit}, guarded to {@code |c| <= 2^40} (design 6.8).
     *
     * @throws ObjectiveRangeException on int64 overflow or when the coefficient exceeds 2^40
     */
    static long coef(long weight, long unit, String what) {
        long c = ScaledMath.mulExact(weight, unit);
        if (c > ScaledMath.MAX_COEFFICIENT || c < -ScaledMath.MAX_COEFFICIENT) {
            throw new ObjectiveRangeException(what + " coefficient exceeds the supported range (2^40): " + c);
        }
        return c;
    }

    /** @return {@code true} every 1024th iteration, the cancellation checkpoint cadence */
    static boolean checkpointDue(int iteration) {
        return (iteration & 1023) == 1023;
    }

    /** Sum of the {@code k} largest of {@code values[0..n)} (sorts the prefix of the array). */
    static long topSum(long[] values, int n, int k) {
        if (n == 0 || k <= 0) {
            return 0;
        }
        Arrays.sort(values, 0, n);
        long sum = 0;
        for (int i = n - 1; i >= Math.max(0, n - k); i--) {
            sum = ScaledMath.addExact(sum, values[i]);
        }
        return sum;
    }

    /** @return the literals {@code cand(c)} of the candidates of {@code task} on {@code resource} (empty when none or {@code resource < 0}) */
    static List<Literal> candidateLiterals(CpModelContext ctx, CandidateIndex index, int task, int resource) {
        List<Literal> lits = new ArrayList<>(2);
        if (resource < 0) {
            return lits;
        }
        for (int c = index.candidateBegin(task); c < index.candidateEnd(task); c++) {
            if (index.candidateResource(c) == resource) {
                lits.add(ctx.cand(c));
            }
        }
        return lits;
    }

    /**
     * Adds {@code coef * (selected(task) - assigned(task, resource))}: {@code coef} when the task is selected and NOT on the
     * resource (an unknown resource, {@code resource < 0}, is never assigned). A required task contributes through the
     * accumulator's constant offset.
     */
    static void notOnResource(CpModelContext ctx, CandidateIndex index, Accumulator acc, int task, int resource, long coef) {
        if (coef == 0) {
            return;
        }
        List<Literal> lits = candidateLiterals(ctx, index, task, resource);
        boolean required = index.required(task);
        if (lits.isEmpty()) {
            if (required) {
                acc.offset(coef);
            } else {
                acc.add(ctx.presence(task), coef, 0, coef);
            }
            return;
        }
        if (required) {
            for (Literal lit : lits) {
                acc.term(lit, -coef);
            }
            acc.bounds(-coef, 0);
            acc.offset(coef);
        } else {
            acc.term(ctx.presence(task), coef);
            for (Literal lit : lits) {
                acc.term(lit, -coef);
            }
            acc.bounds(0, coef);
        }
    }

    /**
     * Adds {@code coef * |start(task) - preferredTick|} (0 for an unselected task) with one auxiliary deviation variable.
     */
    static void startDeviation(CpModelContext ctx, CandidateIndex index, Accumulator acc, int task, int preferredTick, long coef) {
        if (coef == 0) {
            return;
        }
        long maxDeviation = Math.max(Math.abs((long) index.estStart(task) - preferredTick),
                Math.abs((long) index.lstStart(task) - preferredTick));
        if (maxDeviation == 0) {
            return;
        }
        IntVar deviation = ctx.newTierVar("dev_" + task + "_" + preferredTick, 0, maxDeviation);
        LinearExpr above = LinearExpr.newBuilder().add(ctx.start(task)).add(-(long) preferredTick).build();
        LinearExpr below = LinearExpr.newBuilder().add((long) preferredTick).addTerm(ctx.start(task), -1).build();
        Constraint c1 = ctx.model().addGreaterOrEqual(deviation, above);
        Constraint c2 = ctx.model().addGreaterOrEqual(deviation, below);
        if (!index.required(task)) {
            c1.onlyEnforceIf(ctx.presence(task));
            c2.onlyEnforceIf(ctx.presence(task));
        }
        ctx.count(0, 0, 2, 0);
        acc.add(deviation, coef, 0, ScaledMath.mulExact(coef, maxDeviation));
    }

    /**
     * Collects the terms of one objective (one tier) together with their exact bounds, then hands them to the sink in ONE
     * {@code sink.add}. Bounds are the sum of the bounds the callers declare per group of terms; every sum is guarded to
     * {@code 2^53}.
     */
    static final class Accumulator {
        private final LinearExprBuilder builder = LinearExpr.newBuilder();
        private long lo;
        private long hi;
        private long offset;
        private int terms;

        /** Adds {@code coef * arg} to the expression only (the caller declares the bounds with {@link #bounds}). */
        void term(LinearArgument arg, long coef) {
            builder.addTerm(arg, coef);
            terms++;
        }

        /** Adds the exact bounds of a group of terms added with {@link #term}. */
        void bounds(long termLo, long termHi) {
            lo = ScaledMath.addExact(lo, termLo);
            hi = ScaledMath.addExact(hi, termHi);
        }

        /** Adds one term with its own bounds. */
        void add(LinearArgument arg, long coef, long termLo, long termHi) {
            term(arg, coef);
            bounds(termLo, termHi);
        }

        /** Adds a constant to the tier value (sent through {@code ObjectiveSink.addConstantOffset}). */
        void offset(long value) {
            offset = ScaledMath.addExact(offset, value);
        }

        /** @return {@code true} when no term and no offset was added */
        boolean isEmpty() {
            return terms == 0 && offset == 0;
        }

        /** Sends the collected term (and offset) to the sink; does nothing when empty. */
        void flush(ObjectiveSink sink, int tier, String label) {
            if (terms > 0) {
                sink.add(tier, builder.build(), ScaledMath.guard(lo, label), ScaledMath.guard(hi, label), label);
            }
            if (offset != 0) {
                sink.addConstantOffset(tier, ScaledMath.guard(offset, label));
            }
        }
    }
}
