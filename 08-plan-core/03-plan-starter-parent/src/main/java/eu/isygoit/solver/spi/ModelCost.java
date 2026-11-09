package eu.isygoit.solver.spi;

/**
 * Estimated size of the solver model contributed by a part of the problem (design 7.3), used to enforce limits BEFORE
 * any model is built.
 * <p>
 * Immutable value record; all counters are non-negative and additions saturate at {@link Long#MAX_VALUE}. Thread-safe.
 *
 * @param boolVars    boolean variables
 * @param intVars     integer variables
 * @param constraints model constraints
 * @param intervals   (optional) interval variables
 * @param circuitArcs circuit arc literals (setup times)
 * @param pairwise    pairwise terms (task x task) that would be enumerated, guarded by maxPairwise
 */
public record ModelCost(long boolVars, long intVars, long constraints, long intervals, long circuitArcs, long pairwise) {

    /** The neutral element of {@link #plus(ModelCost)}. */
    public static final ModelCost ZERO = new ModelCost(0, 0, 0, 0, 0, 0);

    /**
     * Component-wise saturating addition.
     *
     * @param o the other cost; {@code null} is treated as {@link #ZERO}
     * @return the sum
     */
    public ModelCost plus(ModelCost o) {
        if (o == null) {
            return this;
        }
        return new ModelCost(
                add(boolVars, o.boolVars),
                add(intVars, o.intVars),
                add(constraints, o.constraints),
                add(intervals, o.intervals),
                add(circuitArcs, o.circuitArcs),
                add(pairwise, o.pairwise));
    }

    private static long add(long a, long b) {
        long sum = a + b;
        // overflow iff both operands have the same sign and the sum has a different sign
        if (((a ^ sum) & (b ^ sum)) < 0) {
            return a < 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
        return sum;
    }
}
