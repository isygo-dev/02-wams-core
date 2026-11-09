package eu.isygoit.solver.cpsat;

import com.google.ortools.sat.LinearExpr;
import eu.isygoit.dto.request.SolverOptions;
import eu.isygoit.dto.request.SolverOptions.LexicographicMode;
import eu.isygoit.exception.ObjectiveRangeException;
import eu.isygoit.util.ScaledMath;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure arithmetic of the tier machinery (design 6.8): mode decision, 2^53 guards, multipliers, products. The static helpers
 * do not touch OR-Tools; the instance tests only use the pure-Java expression classes and are skipped when those cannot be
 * loaded.
 */
class TierObjectiveBuilderTest {

    private static final long TWO_53 = ScaledMath.MAX_EXACT;

    // ------------------------------------------------------------------------------------------- arithmetic

    @Test
    void productAndMultipliersFollowTheRanges() {
        long[] ranges = {4, 9, 2};
        assertEquals(BigInteger.valueOf(5 * 10 * 3), TierObjectiveBuilder.product(ranges));
        // M_last = 1, M_1 = 1 * (2 + 1), M_0 = 3 * (9 + 1)
        assertArrayEquals(new long[]{30, 3, 1}, TierObjectiveBuilder.multipliers(ranges));
    }

    @Test
    void emptyAndSingleTierHaveUnitMultiplier() {
        assertEquals(BigInteger.ONE, TierObjectiveBuilder.product(new long[0]));
        assertArrayEquals(new long[]{1}, TierObjectiveBuilder.multipliers(new long[]{12345}));
    }

    @Test
    void multiplierOverflowIsRejected() {
        // (2^30) * (2^30) > 2^53 : the multiplier of tier 0 would exceed the exact range
        long[] ranges = {5, 1L << 30, 1L << 30};
        assertThrows(ObjectiveRangeException.class, () -> TierObjectiveBuilder.multipliers(ranges));
    }

    @Test
    void autoChoosesWeightedWhenTheProductFits() {
        long[] lows = {0, 0, 0};
        long[] highs = {99, 999, 9};
        assertEquals(LexicographicMode.WEIGHTED, TierObjectiveBuilder.decideMode(LexicographicMode.AUTO, lows, highs));
        assertEquals(LexicographicMode.WEIGHTED, TierObjectiveBuilder.decideMode(null, lows, highs));
    }

    @Test
    void autoFallsBackToSequentialWhenTheProductExceeds2To53() {
        long[] lows = {0, 0};
        long[] highs = {1L << 30, 1L << 30};
        assertTrue(TierObjectiveBuilder.product(new long[]{1L << 30, 1L << 30}).compareTo(BigInteger.valueOf(TWO_53)) > 0);
        assertEquals(LexicographicMode.SEQUENTIAL, TierObjectiveBuilder.decideMode(LexicographicMode.AUTO, lows, highs));
    }

    @Test
    void forcedWeightedThatDoesNotFitFailsLoudly() {
        long[] lows = {0, 0};
        long[] highs = {1L << 30, 1L << 30};
        assertThrows(ObjectiveRangeException.class,
                () -> TierObjectiveBuilder.decideMode(LexicographicMode.WEIGHTED, lows, highs));
    }

    @Test
    void forcedSequentialIsKeptForSeveralTiersAndCollapsesForOne() {
        assertEquals(LexicographicMode.SEQUENTIAL,
                TierObjectiveBuilder.decideMode(LexicographicMode.SEQUENTIAL, new long[]{0, 0}, new long[]{1, 1}));
        assertEquals(LexicographicMode.WEIGHTED,
                TierObjectiveBuilder.decideMode(LexicographicMode.SEQUENTIAL, new long[]{0}, new long[]{1}));
        assertEquals(LexicographicMode.WEIGHTED,
                TierObjectiveBuilder.decideMode(LexicographicMode.AUTO, new long[0], new long[0]));
    }

    @Test
    void weightedBoundUsesTheLargestMagnitudeOfEachTier() {
        long[] lows = {-10, 0};
        long[] highs = {5, 3};
        long[] multipliers = {4, 1};
        // 4 * max(|-10|, |5|) + 1 * max(0, 3) = 43
        assertEquals(BigInteger.valueOf(43), TierObjectiveBuilder.weightedBound(lows, highs, multipliers));
    }

    @Test
    void autoFallsBackWhenTheWeightedObjectiveCouldExceedInt64() {
        // ranges are tiny (product 11 * 601 fits) but the offsets are huge: M_0 = 601 > 512 = 2^62 / 2^53, so the weighted
        // objective could exceed what CP-SAT represents and AUTO must choose SEQUENTIAL
        long[] lows = {TWO_53 - 10, TWO_53 - 600};
        long[] highs = {TWO_53, TWO_53};
        assertTrue(TierObjectiveBuilder.product(new long[]{10, 600}).compareTo(BigInteger.valueOf(TWO_53)) < 0);
        assertEquals(LexicographicMode.SEQUENTIAL, TierObjectiveBuilder.decideMode(null, lows, highs));
    }

    // ------------------------------------------------------------------------------------------- the builder

    @Test
    void plansTheTiersOfOffsetTermsInAscendingOrder() {
        Assumptions.assumeTrue(expressionsUsable(), "OR-Tools expression classes not loadable");
        TierObjectiveBuilder builder = new TierObjectiveBuilder(8);
        builder.addConstantOffset(1, 7);
        builder.addConstantOffset(-2, 1000);
        builder.add(0, LinearExpr.constant(5), 5, 5, "five");
        assertArrayEquals(new int[]{-2, 0, 1}, builder.tiers());
        TierObjectiveBuilder.Plan plan = builder.plan(SolverOptions.LexicographicMode.AUTO);
        assertEquals(LexicographicMode.WEIGHTED, plan.mode());
        assertArrayEquals(new long[]{0, 0, 0}, plan.ranges());
        assertArrayEquals(new long[]{1, 1, 1}, plan.multipliers());
    }

    @Test
    void rejectsAnOffsetAbove2To53() {
        Assumptions.assumeTrue(expressionsUsable(), "OR-Tools expression classes not loadable");
        TierObjectiveBuilder builder = new TierObjectiveBuilder(8);
        assertThrows(ObjectiveRangeException.class, () -> builder.addConstantOffset(0, TWO_53 + 1));
    }

    @Test
    void rejectsMoreDistinctUserTiersThanAllowed() {
        Assumptions.assumeTrue(expressionsUsable(), "OR-Tools expression classes not loadable");
        TierObjectiveBuilder builder = new TierObjectiveBuilder(2);
        builder.addConstantOffset(0, 1);
        builder.addConstantOffset(1, 1);
        builder.addConstantOffset(2, 1);
        builder.addConstantOffset(-1, 1); // reserved tiers are not counted
        assertThrows(ObjectiveRangeException.class, () -> builder.plan(null));
    }

    @Test
    void rejectsATierRangeAbove2To53() {
        Assumptions.assumeTrue(expressionsUsable(), "OR-Tools expression classes not loadable");
        TierObjectiveBuilder builder = new TierObjectiveBuilder(8);
        builder.add(0, LinearExpr.constant(0), 0, TWO_53, "wide");
        assertThrows(ObjectiveRangeException.class, () -> builder.add(0, LinearExpr.constant(0), 0, 1, "wider"));
    }

    private static boolean expressionsUsable() {
        try {
            LinearExpr.newBuilder().add(1L).build();
            return true;
        } catch (Throwable unusable) {
            return false;
        }
    }
}
