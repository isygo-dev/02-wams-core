package eu.isygoit.solver.cpsat;

import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;
import com.google.ortools.sat.Literal;
import eu.isygoit.dto.request.SolverOptions;
import eu.isygoit.exception.ObjectiveRangeException;
import eu.isygoit.util.ScaledMath;

import java.math.BigInteger;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Collects the lexicographic objective terms of one build (user objectives, SOFT penalties, relaxation tier -2, automatic
 * selection tier -1) and decides how the tiers are optimised (design 6.8, D-9, D-12).
 * <p>
 * <b>Arithmetic contract.</b> Every tier {@code k} has an expression {@code expr_k} with exact bounds {@code [lo_k, hi_k]}
 * (sums of the bounds passed to {@link #add}); its range is {@code R_k = hi_k - lo_k}. Guards (violation throws
 * {@link ObjectiveRangeException}, code {@code OBJECTIVE_SCORE_OUT_OF_RANGE}): every added coefficient {@code |c| <= 2^40},
 * every bound and every {@code R_k <= 2^53}, the number of distinct user tiers ({@code >= 0}) {@code <= maxTiers}.
 * <ul>
 *   <li>AUTO: the product {@code P = prod(R_k + 1)} is computed in {@link BigInteger}; WEIGHTED when {@code P <= 2^53}
 *   (and the weighted objective stays below 2^62), else SEQUENTIAL.</li>
 *   <li>WEIGHTED: {@code M_last = 1}, {@code M_k = M_(k+1) * (R_(k+1) + 1)}, objective {@code sum_k M_k * expr_k}.</li>
 *   <li>SEQUENTIAL: tier after tier (see {@code SolveRunner}).</li>
 * </ul>
 * The pure arithmetic ({@link #decideMode}, {@link #product}, {@link #multipliers}, {@link #weightedBound}) is static and
 * does not touch OR-Tools, so it is unit-testable without natives.
 * <p>
 * Thread-safety: single building thread. Units: scaled integers (decimal inputs times the objective scale) or exact
 * ticks/counts.
 */
public final class TierObjectiveBuilder implements ObjectiveSink {

    /** Reserved tier of the relaxation cost. */
    public static final int TIER_RELAXATION = -2;
    /** Reserved tier of the automatic selection value. */
    public static final int TIER_AUTO_SELECTION = -1;

    private static final BigInteger LIMIT_2_53 = BigInteger.valueOf(ScaledMath.MAX_EXACT);
    private static final BigInteger LIMIT_2_62 = BigInteger.ONE.shiftLeft(62);

    private final int maxTiers;
    private final TreeMap<Integer, Accumulator> tiers = new TreeMap<>();
    private int termCount;

    /** Result of {@link #plan}: how the tiers are optimised. All arrays are indexed by position in {@link #tiers()}. */
    public record Plan(SolverOptions.LexicographicMode mode, int[] tiers, long[] lows, long[] highs, long[] ranges,
                       long[] multipliers) {
        /** @return the number of tiers */
        public int tierCount() {
            return tiers.length;
        }
    }

    /**
     * @param maxTiers maximum number of distinct user tiers ({@code >= 0}); {@link Integer#MAX_VALUE} disables the guard
     */
    public TierObjectiveBuilder(int maxTiers) {
        this.maxTiers = maxTiers;
    }

    // ------------------------------------------------------------------------------------------------------ sink

    @Override
    public void add(int tier, LinearExpr expr, long minValue, long maxValue, String label) {
        Objects.requireNonNull(expr, "expr");
        if (minValue > maxValue) {
            throw new IllegalArgumentException("objective term '" + label + "' has min > max");
        }
        ScaledMath.guard(minValue, "objective term '" + label + "' minimum");
        ScaledMath.guard(maxValue, "objective term '" + label + "' maximum");
        long maxCoefficient = 0L;
        int elements = expr.numElements();
        for (int i = 0; i < elements; i++) {
            long coefficient = expr.getCoefficient(i);
            if (coefficient > ScaledMath.MAX_COEFFICIENT || coefficient < -ScaledMath.MAX_COEFFICIENT) {
                throw new ObjectiveRangeException("objective term '" + label + "' has a coefficient above 2^40: "
                        + coefficient);
            }
            maxCoefficient = Math.max(maxCoefficient, Math.abs(coefficient));
        }
        Accumulator accumulator = accumulator(tier);
        accumulator.builder.add(expr);
        accumulator.include(minValue, maxValue, maxCoefficient, label);
        termCount++;
    }

    @Override
    public void addConstantOffset(int tier, long value) {
        ScaledMath.guard(value, "objective constant offset");
        Accumulator accumulator = accumulator(tier);
        accumulator.builder.add(value);
        accumulator.include(value, value, 0L, "offset");
    }

    /**
     * Fast path for the SOFT penalty of one unit literal: adds {@code weight * (1 - satisfied)} to the tier.
     *
     * @param tier      the constraint tier
     * @param satisfied the literal that is true when the instance is satisfied
     * @param weight    the penalty paid when {@code satisfied} is false, {@code >= 0}
     */
    public void addSoftUnit(int tier, Literal satisfied, long weight) {
        if (weight < 0 || weight > ScaledMath.MAX_COEFFICIENT) {
            throw new ObjectiveRangeException("soft weight out of range: " + weight);
        }
        Accumulator accumulator = accumulator(tier);
        accumulator.builder.addTerm(satisfied.not(), weight);
        accumulator.include(0L, weight, weight, "soft");
        termCount++;
    }

    /** @return the number of terms added so far (for statistics) */
    public int termCount() {
        return termCount;
    }

    /** @return true when no term and no offset was added to any tier */
    public boolean isEmpty() {
        return tiers.isEmpty();
    }

    // ------------------------------------------------------------------------------------------------------ plan

    /** @return the distinct tiers in ascending order (most important first) */
    public int[] tiers() {
        int[] result = new int[tiers.size()];
        int i = 0;
        for (Integer tier : tiers.keySet()) {
            result[i++] = tier;
        }
        return result;
    }

    /**
     * Decides the optimisation mode and computes the multipliers.
     *
     * @param requested the requested {@code lexicographicMode} (null = AUTO)
     * @return the plan; for SEQUENTIAL the multipliers array is empty
     * @throws ObjectiveRangeException when a guard is violated or WEIGHTED was forced but does not fit
     */
    public Plan plan(SolverOptions.LexicographicMode requested) {
        int[] ids = tiers();
        int userTiers = 0;
        for (int tier : ids) {
            if (tier >= 0) {
                userTiers++;
            }
        }
        if (userTiers > maxTiers) {
            throw new ObjectiveRangeException("number of distinct tiers " + userTiers + " exceeds the limit " + maxTiers);
        }
        long[] lows = new long[ids.length];
        long[] highs = new long[ids.length];
        long[] ranges = new long[ids.length];
        long[] coefficients = new long[ids.length];
        for (int i = 0; i < ids.length; i++) {
            Accumulator accumulator = tiers.get(ids[i]);
            lows[i] = accumulator.lo;
            highs[i] = accumulator.hi;
            ranges[i] = Math.subtractExact(accumulator.hi, accumulator.lo);
            coefficients[i] = accumulator.maxCoefficient;
            if (ranges[i] > ScaledMath.MAX_EXACT) {
                throw new ObjectiveRangeException("tier " + ids[i] + " range exceeds 2^53: " + ranges[i]);
            }
        }
        SolverOptions.LexicographicMode mode = decideMode(requested, lows, highs);
        long[] multipliers = new long[0];
        if (mode == SolverOptions.LexicographicMode.WEIGHTED) {
            multipliers = multipliers(ranges);
            for (int i = 0; i < ids.length; i++) {
                BigInteger scaled = BigInteger.valueOf(multipliers[i]).multiply(BigInteger.valueOf(coefficients[i]));
                if (scaled.compareTo(LIMIT_2_62) > 0) {
                    throw new ObjectiveRangeException("tier " + ids[i] + " weighted coefficient exceeds 2^62");
                }
            }
        }
        return new Plan(mode, ids, lows, highs, ranges, multipliers);
    }

    /**
     * The expression of a tier (the sum of everything added to it), built once and cached.
     *
     * @param tier a tier returned by {@link #tiers()}
     * @return the tier expression
     */
    public LinearExpr expr(int tier) {
        Accumulator accumulator = tiers.get(tier);
        if (accumulator == null) {
            throw new IllegalArgumentException("unknown tier " + tier);
        }
        if (accumulator.built == null) {
            accumulator.built = accumulator.builder.build();
        }
        return accumulator.built;
    }

    /**
     * The WEIGHTED objective {@code sum_k M_k * expr_k}.
     *
     * @param plan a plan computed by {@link #plan} with mode WEIGHTED
     * @return the objective expression to minimise
     */
    public LinearExpr weightedObjective(Plan plan) {
        if (plan.mode() != SolverOptions.LexicographicMode.WEIGHTED) {
            throw new IllegalStateException("weightedObjective needs a WEIGHTED plan");
        }
        LinearExprBuilder objective = LinearExpr.newBuilder();
        for (int i = 0; i < plan.tiers().length; i++) {
            objective.addTerm(expr(plan.tiers()[i]), plan.multipliers()[i]);
        }
        return objective.build();
    }

    // ------------------------------------------------------------------------------------------ pure arithmetic

    /**
     * {@code P = prod(R_k + 1)} in arbitrary precision.
     *
     * @param ranges the tier ranges {@code R_k}, each {@code >= 0}
     * @return the product (1 for no tier)
     */
    public static BigInteger product(long[] ranges) {
        BigInteger result = BigInteger.ONE;
        for (long range : ranges) {
            if (range < 0) {
                throw new IllegalArgumentException("tier range must not be negative: " + range);
            }
            result = result.multiply(BigInteger.valueOf(range).add(BigInteger.ONE));
        }
        return result;
    }

    /**
     * Tier multipliers {@code M_last = 1}, {@code M_k = M_(k+1) * (R_(k+1) + 1)}.
     *
     * @param ranges the tier ranges in tier order (most important first)
     * @return the multipliers in the same order
     * @throws ObjectiveRangeException when a multiplier exceeds 2^53
     */
    public static long[] multipliers(long[] ranges) {
        long[] result = new long[ranges.length];
        BigInteger multiplier = BigInteger.ONE;
        for (int i = ranges.length - 1; i >= 0; i--) {
            if (multiplier.compareTo(LIMIT_2_53) > 0) {
                throw new ObjectiveRangeException("tier multiplier exceeds 2^53 at position " + i);
            }
            result[i] = multiplier.longValueExact();
            multiplier = multiplier.multiply(BigInteger.valueOf(ranges[i]).add(BigInteger.ONE));
        }
        return result;
    }

    /**
     * Upper bound of the absolute value of the WEIGHTED objective: {@code sum_k M_k * max(|lo_k|, |hi_k|)}.
     *
     * @param lows        tier minima
     * @param highs       tier maxima
     * @param multipliers tier multipliers
     * @return the bound in arbitrary precision
     */
    public static BigInteger weightedBound(long[] lows, long[] highs, long[] multipliers) {
        BigInteger bound = BigInteger.ZERO;
        for (int i = 0; i < multipliers.length; i++) {
            BigInteger magnitude = BigInteger.valueOf(lows[i]).abs().max(BigInteger.valueOf(highs[i]).abs());
            bound = bound.add(BigInteger.valueOf(multipliers[i]).multiply(magnitude));
        }
        return bound;
    }

    /**
     * Mode decision of design 6.8.
     * <ul>
     *   <li>AUTO (or null): WEIGHTED when {@code P <= 2^53} and the weighted objective bound is below 2^62, else SEQUENTIAL.</li>
     *   <li>WEIGHTED: the same test, but a violation throws {@link ObjectiveRangeException}.</li>
     *   <li>SEQUENTIAL: SEQUENTIAL; with at most one tier the result is WEIGHTED (identical, cheaper).</li>
     * </ul>
     *
     * @param requested the requested mode (null = AUTO)
     * @param lows      tier minima, most important tier first
     * @param highs     tier maxima
     * @return the effective mode (never AUTO)
     */
    public static SolverOptions.LexicographicMode decideMode(
            SolverOptions.LexicographicMode requested, long[] lows, long[] highs) {
        SolverOptions.LexicographicMode mode =
                requested == null ? SolverOptions.LexicographicMode.AUTO : requested;
        int count = lows.length;
        if (count <= 1) {
            return SolverOptions.LexicographicMode.WEIGHTED;
        }
        if (mode == SolverOptions.LexicographicMode.SEQUENTIAL) {
            return SolverOptions.LexicographicMode.SEQUENTIAL;
        }
        long[] ranges = new long[count];
        for (int i = 0; i < count; i++) {
            ranges[i] = Math.subtractExact(highs[i], lows[i]);
        }
        boolean fits = product(ranges).compareTo(LIMIT_2_53) <= 0;
        if (fits) {
            long[] multipliers = multipliers(ranges);
            fits = weightedBound(lows, highs, multipliers).compareTo(LIMIT_2_62) <= 0;
        }
        if (mode == SolverOptions.LexicographicMode.WEIGHTED) {
            if (!fits) {
                throw new ObjectiveRangeException(
                        "lexicographic mode WEIGHTED does not fit in 2^53: the tiers need SEQUENTIAL (or AUTO)");
            }
            return SolverOptions.LexicographicMode.WEIGHTED;
        }
        return fits ? SolverOptions.LexicographicMode.WEIGHTED : SolverOptions.LexicographicMode.SEQUENTIAL;
    }

    private Accumulator accumulator(int tier) {
        return tiers.computeIfAbsent(tier, ignored -> new Accumulator());
    }

    /** Per tier state. */
    private static final class Accumulator {
        private final LinearExprBuilder builder = LinearExpr.newBuilder();
        private LinearExpr built;
        private long lo;
        private long hi;
        private long maxCoefficient;

        void include(long min, long max, long coefficient, String label) {
            if (built != null) {
                throw new IllegalStateException("tier already finalised; cannot add '" + label + "'");
            }
            lo = ScaledMath.addExact(lo, min);
            hi = ScaledMath.addExact(hi, max);
            ScaledMath.guard(lo, "tier minimum");
            ScaledMath.guard(hi, "tier maximum");
            if (hi - lo > ScaledMath.MAX_EXACT) {
                throw new ObjectiveRangeException("tier range exceeds 2^53 after term '" + label + "'");
            }
            maxCoefficient = Math.max(maxCoefficient, coefficient);
        }
    }
}
