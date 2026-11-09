package eu.isygoit.solver.cpsat.explain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.LongSupplier;

/**
 * Deletion-filter minimisation of an infeasible assumption set (design 6.10 phase B, shrink step). Solver independent:
 * the infeasibility test is an injected {@link Oracle}, which makes the algorithm unit-testable without natives.
 * <p>
 * For each of the first {@code maxItems} items of the core, in order: ask the oracle whether the set WITHOUT the item
 * is still infeasible. If yes the item is dropped, and the set is replaced by the (possibly smaller) subset the oracle
 * returned; if the oracle proves feasibility the item is necessary and stays; if the oracle cannot decide (time limit)
 * the item stays and the result is not minimal. The loop stops when the deadline passes. {@code minimal} is true only
 * if every item of the final core was examined with a conclusive answer: that requires the core to have at most
 * {@code maxItems} items at the start of the examination of its last element, no inconclusive answer and no timeout.
 * <p>
 * Items are opaque ints (indices into the assumption list). Not thread-safe; one instance per explanation.
 */
public final class DeletionFilter {

    /** Verdict of the oracle on a candidate subset. */
    public enum Kind {
        /** The subset is proven infeasible; {@link Verdict#subset()} is an infeasible subset of it. */
        INFEASIBLE,
        /** The subset is feasible (or at least satisfiable): the removed item is necessary. */
        SATISFIABLE,
        /** No answer within the limit. */
        UNKNOWN
    }

    /**
     * @param kind   the verdict
     * @param subset for {@link Kind#INFEASIBLE}: a subset of the tested set that is still infeasible; {@code null} or
     *               empty means "use the whole tested set"
     */
    public record Verdict(Kind kind, int[] subset) {
        /** @return an INFEASIBLE verdict */
        public static Verdict infeasible(int[] subset) {
            return new Verdict(Kind.INFEASIBLE, subset);
        }

        /** @return a SATISFIABLE verdict */
        public static Verdict satisfiable() {
            return new Verdict(Kind.SATISFIABLE, null);
        }

        /** @return an UNKNOWN verdict */
        public static Verdict unknown() {
            return new Verdict(Kind.UNKNOWN, null);
        }
    }

    /** Decides whether a set of assumption indices is infeasible. */
    @FunctionalInterface
    public interface Oracle {
        /**
         * @param subset the candidate set (never modified by the oracle)
         * @return the verdict
         */
        Verdict test(int[] subset);
    }

    /**
     * @param core        the final core (ascending order not required)
     * @param minimal     whether it is proven minimal
     * @param oracleCalls number of oracle invocations
     */
    public record Result(int[] core, boolean minimal, int oracleCalls) {
    }

    private final int maxItems;
    private final long deadlineNanos;
    private final LongSupplier nanoClock;

    /**
     * @param maxItems      maximum number of core items to examine (design: 30)
     * @param deadlineNanos absolute deadline in the clock's nanoseconds
     * @param nanoClock     clock ({@code System::nanoTime} in production)
     */
    public DeletionFilter(int maxItems, long deadlineNanos, LongSupplier nanoClock) {
        this.maxItems = Math.max(0, maxItems);
        this.deadlineNanos = deadlineNanos;
        this.nanoClock = nanoClock;
    }

    /**
     * Shrinks the core.
     *
     * @param core   an infeasible set of items
     * @param oracle the infeasibility test
     * @return the shrunk core
     */
    public Result shrink(int[] core, Oracle oracle) {
        Set<Integer> current = new LinkedHashSet<>();
        for (int item : core) {
            current.add(item);
        }
        List<Integer> examineOrder = new ArrayList<>(current);
        int calls = 0;
        boolean allConclusive = true;
        boolean complete = true;
        int examined = 0;
        for (int item : examineOrder) {
            if (examined >= maxItems || nanoClock.getAsLong() >= deadlineNanos) {
                complete = false;
                break;
            }
            examined++;
            if (!current.contains(item)) {
                continue; // already removed by a previous, smaller subset
            }
            int[] without = new int[current.size() - 1];
            int n = 0;
            for (int other : current) {
                if (other != item) {
                    without[n++] = other;
                }
            }
            Verdict verdict = oracle.test(without);
            calls++;
            switch (verdict.kind()) {
                case INFEASIBLE -> {
                    int[] subset = verdict.subset();
                    Set<Integer> next = new LinkedHashSet<>();
                    if (subset == null || subset.length == 0) {
                        for (int value : without) {
                            next.add(value);
                        }
                    } else {
                        for (int value : subset) {
                            if (current.contains(value) && value != item) {
                                next.add(value);
                            }
                        }
                        if (next.isEmpty()) {
                            for (int value : without) {
                                next.add(value);
                            }
                        }
                    }
                    current = next;
                }
                case SATISFIABLE -> {
                    // necessary item: keep
                }
                default -> allConclusive = false;
            }
        }
        int[] out = new int[current.size()];
        int pos = 0;
        for (int value : current) {
            out[pos++] = value;
        }
        return new Result(out, complete && allConclusive, calls);
    }
}
