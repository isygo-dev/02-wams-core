package eu.isygoit.util;

import eu.isygoit.exception.ObjectiveRangeException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The single place where decimal objective inputs (costs, prices, penalties) become integers, shared by the
 * engines, the independent solution validator and the Timefold adapter so that EXACTLY the same number is computed
 * everywhere (decision D-12).
 * <p>
 * Rules:
 * <ul>
 *   <li>A decimal input {@code v} is multiplied by {@code scale} (a power-of-ten-like multiplier, 1..1_000_000)
 *       and rounded HALF-UP on the exact decimal value of {@code v} (the shortest decimal representation of the
 *       double, so {@code 2.675 * 100} gives 268, not 267 as binary floating point would).</li>
 *   <li>Integral measures (ticks, counts) are NEVER scaled.</li>
 *   <li>Every scaled value is guarded to {@code |x| <= 2^53} ({@link #MAX_EXACT}): exactly representable in JSON
 *       doubles and JavaScript. Anything larger, non-finite or (for {@link #scale}) negative raises
 *       {@link ObjectiveRangeException}.</li>
 *   <li>{@link #addExact} / {@link #mulExact} are int64 exact wrappers that raise {@link ObjectiveRangeException}
 *       instead of {@link ArithmeticException}.</li>
 * </ul>
 */
public final class ScaledMath {

    /**
     * Largest magnitude of a scaled value, tier value or weighted total: 2^53.
     */
    public static final long MAX_EXACT = 1L << 53;

    /**
     * Largest magnitude of a single objective coefficient: 2^40.
     */
    public static final long MAX_COEFFICIENT = 1L << 40;

    private ScaledMath() {
    }

    /**
     * Scales a non-negative decimal input: {@code round_half_up(v * scale)}.
     *
     * @param v     finite and {@code >= 0}
     * @param scale multiplier, {@code >= 1}
     * @return the scaled value, in {@code [0, 2^53]}
     * @throws ObjectiveRangeException when {@code v} is not finite, negative, or the result exceeds 2^53
     */
    public static long scale(double v, int scale) {
        requireFinite(v);
        if (v < 0.0) {
            throw new ObjectiveRangeException("value " + v + " must not be negative");
        }
        return scaleExact(BigDecimal.valueOf(v), scale);
    }

    /**
     * Scales a decimal input that may be negative (explicit reward inputs): half-up on the absolute value, sign
     * kept.
     *
     * @param v     finite
     * @param scale multiplier, {@code >= 1}
     * @return the scaled value, in {@code [-2^53, 2^53]}
     * @throws ObjectiveRangeException when {@code v} is not finite or the result exceeds 2^53 in magnitude
     */
    public static long scaleSigned(double v, int scale) {
        requireFinite(v);
        return scaleExact(BigDecimal.valueOf(v), scale);
    }

    /**
     * Scales an exact decimal: {@code round_half_up(v * scale)} (half away from zero for negative values).
     *
     * @param v     the decimal (non-null)
     * @param scale multiplier, {@code >= 1}
     * @return the scaled value, in {@code [-2^53, 2^53]}
     * @throws ObjectiveRangeException when the result exceeds 2^53 in magnitude
     */
    public static long scale(BigDecimal v, int scale) {
        return scaleExact(v, scale);
    }

    /**
     * Human-facing value of a scaled integer: {@code raw / scale}.
     *
     * @param raw   scaled integer
     * @param scale the multiplier it was scaled with, {@code >= 1}
     * @return the decimal value as a double
     */
    public static double unscale(long raw, int scale) {
        requireScale(scale);
        return (double) raw / (double) scale;
    }

    /**
     * Exact int64 addition.
     *
     * @throws ObjectiveRangeException on overflow
     */
    public static long addExact(long a, long b) {
        try {
            return Math.addExact(a, b);
        } catch (ArithmeticException e) {
            throw new ObjectiveRangeException("objective sum overflows int64: " + a + " + " + b, e);
        }
    }

    /**
     * Exact int64 multiplication.
     *
     * @throws ObjectiveRangeException on overflow
     */
    public static long mulExact(long a, long b) {
        try {
            return Math.multiplyExact(a, b);
        } catch (ArithmeticException e) {
            throw new ObjectiveRangeException("objective product overflows int64: " + a + " * " + b, e);
        }
    }

    /**
     * Checks that a value stays within {@code [-2^53, 2^53]}.
     *
     * @param value the value (tier value, weighted total, scaled term)
     * @param what  what is checked, for the error message
     * @return {@code value}
     * @throws ObjectiveRangeException when {@code |value| > 2^53}
     */
    public static long guard(long value, String what) {
        if (value > MAX_EXACT || value < -MAX_EXACT) {
            throw new ObjectiveRangeException(what + " exceeds the supported range (2^53): " + value);
        }
        return value;
    }

    private static long scaleExact(BigDecimal v, int scale) {
        requireScale(scale);
        BigDecimal scaled = v.multiply(BigDecimal.valueOf(scale)).setScale(0, RoundingMode.HALF_UP);
        if (scaled.abs().compareTo(BigDecimal.valueOf(MAX_EXACT)) > 0) {
            throw new ObjectiveRangeException("scaled value " + scaled.toPlainString()
                    + " exceeds the supported range (2^53); lower the objective scale or the input");
        }
        return scaled.longValueExact();
    }

    private static void requireFinite(double v) {
        if (!Double.isFinite(v)) {
            throw new ObjectiveRangeException("value " + v + " is not finite");
        }
    }

    private static void requireScale(int scale) {
        if (scale < 1) {
            throw new IllegalArgumentException("scale must be >= 1, got " + scale);
        }
    }
}
