package eu.isygoit.util;

import eu.isygoit.exception.ObjectiveRangeException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScaledMathTest {

    @Test
    void roundsHalfUpOnTheDecimalValue() {
        assertEquals(268L, ScaledMath.scale(2.675, 100), "binary double 2.675*100 would give 267");
        assertEquals(101L, ScaledMath.scale(1.005, 100));
        assertEquals(1L, ScaledMath.scale(0.5, 1));
        assertEquals(2L, ScaledMath.scale(1.5, 1));
        assertEquals(3L, ScaledMath.scale(2.5, 1));
        assertEquals(0L, ScaledMath.scale(0.4, 1));
        assertEquals(0L, ScaledMath.scale(0.0, 100));
        assertEquals(100L, ScaledMath.scale(1.0, 100));
        assertEquals(1000L, ScaledMath.scale(1.0, 1000));
        assertEquals(200L, ScaledMath.scale(2.0, 100));
        assertEquals(30000L, ScaledMath.scale(30.0, 1000));
    }

    @Test
    void scaleOfBigDecimal() {
        assertEquals(125L, ScaledMath.scale(new BigDecimal("0.125"), 1000));
        assertEquals(13L, ScaledMath.scale(new BigDecimal("0.125"), 100));
        assertEquals(-13L, ScaledMath.scale(new BigDecimal("-0.125"), 100));
    }

    @Test
    void signedInputsKeepTheirSign() {
        assertEquals(-3L, ScaledMath.scaleSigned(-2.5, 1));
        assertEquals(-268L, ScaledMath.scaleSigned(-2.675, 100));
        assertEquals(268L, ScaledMath.scaleSigned(2.675, 100));
        assertEquals(0L, ScaledMath.scaleSigned(-0.0, 100));
    }

    @Test
    void rejectsNegativeNonFiniteAndOverflow() {
        assertThrows(ObjectiveRangeException.class, () -> ScaledMath.scale(-0.01, 100));
        assertThrows(ObjectiveRangeException.class, () -> ScaledMath.scale(Double.NaN, 100));
        assertThrows(ObjectiveRangeException.class, () -> ScaledMath.scale(Double.POSITIVE_INFINITY, 100));
        assertThrows(ObjectiveRangeException.class, () -> ScaledMath.scaleSigned(Double.NEGATIVE_INFINITY, 100));
        assertThrows(ObjectiveRangeException.class, () -> ScaledMath.scale(1e14, 100));
        assertThrows(ObjectiveRangeException.class, () -> ScaledMath.scale(1e300, 1_000_000));
        assertThrows(ObjectiveRangeException.class, () -> ScaledMath.scaleSigned(-1e14, 100));
        assertThrows(ObjectiveRangeException.class,
                () -> ScaledMath.scale(new BigDecimal("9007199254740993"), 1));
    }

    @Test
    void boundaryOfTheExactRangeIsAccepted() {
        assertEquals(ScaledMath.MAX_EXACT, ScaledMath.scale(new BigDecimal("9007199254740992"), 1));
        assertEquals(-ScaledMath.MAX_EXACT, ScaledMath.scale(new BigDecimal("-9007199254740992"), 1));
        assertEquals(ScaledMath.MAX_EXACT, ScaledMath.guard(ScaledMath.MAX_EXACT, "tier"));
        assertEquals(-ScaledMath.MAX_EXACT, ScaledMath.guard(-ScaledMath.MAX_EXACT, "tier"));
    }

    @Test
    void guardRejectsAbove2Pow53() {
        assertThrows(ObjectiveRangeException.class, () -> ScaledMath.guard(ScaledMath.MAX_EXACT + 1, "tier"));
        assertThrows(ObjectiveRangeException.class, () -> ScaledMath.guard(-ScaledMath.MAX_EXACT - 1, "tier"));
    }

    @Test
    void exactArithmeticWrapsOverflow() {
        assertEquals(7L, ScaledMath.addExact(3L, 4L));
        assertEquals(12L, ScaledMath.mulExact(3L, 4L));
        ObjectiveRangeException add = assertThrows(ObjectiveRangeException.class,
                () -> ScaledMath.addExact(Long.MAX_VALUE, 1L));
        assertEquals("OBJECTIVE_SCORE_OUT_OF_RANGE", add.code());
        assertThrows(ObjectiveRangeException.class, () -> ScaledMath.mulExact(Long.MAX_VALUE, 2L));
        assertThrows(ObjectiveRangeException.class, () -> ScaledMath.mulExact(Long.MIN_VALUE, -1L));
    }

    @Test
    void unscale() {
        assertEquals(2.68, ScaledMath.unscale(268L, 100), 1e-12);
        assertEquals(190.0, ScaledMath.unscale(190L, 1), 0.0);
        assertEquals(-1.5, ScaledMath.unscale(-150L, 100), 1e-12);
    }

    @Test
    void scaleMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> ScaledMath.scale(1.0, 0));
        assertThrows(IllegalArgumentException.class, () -> ScaledMath.scale(1.0, -5));
        assertThrows(IllegalArgumentException.class, () -> ScaledMath.unscale(1L, 0));
    }
}
