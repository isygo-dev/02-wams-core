package eu.isygoit.dto.request;

import eu.isygoit.exception.TimeNotAlignedException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HorizonTest {

    private static final Instant START = Instant.parse("2026-09-07T06:00:00Z");
    /** 105 ticks of 900 s (the school timetable horizon of the design). */
    private static final Horizon H = new Horizon(START, Instant.parse("2026-09-08T08:15:00Z"), 900);

    @Test
    void horizonTicks() {
        assertEquals(105L, H.horizonTicks());
        assertEquals(480L, new Horizon(Instant.parse("2026-09-07T08:00:00Z"),
                Instant.parse("2026-09-07T16:00:00Z"), 60).horizonTicks());
        assertEquals(20L, new Horizon(Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-09-21T00:00:00Z"), 86_400).horizonTicks());
    }

    @Test
    void isAligned() {
        assertTrue(H.isAligned(START));
        assertTrue(H.isAligned(Instant.parse("2026-09-07T06:15:00Z")));
        assertTrue(H.isAligned(Instant.parse("2026-09-07T05:45:00Z")), "aligned before the start too");
        assertFalse(H.isAligned(Instant.parse("2026-09-07T06:07:30Z")));
        assertFalse(H.isAligned(START.plusMillis(500)));
        assertFalse(H.isAligned(START.plusMillis(-500)));
        assertFalse(H.isAligned(null));
    }

    @Test
    void toTickExactAndBounds() {
        assertEquals(0, H.toTick(START));
        assertEquals(1, H.toTick(Instant.parse("2026-09-07T06:15:00Z")));
        assertEquals(5, H.toTick(Instant.parse("2026-09-07T07:15:00Z")));
        assertEquals(96, H.toTick(Instant.parse("2026-09-08T06:00:00Z")));
        assertEquals(105, H.toTick(H.end()), "the end boundary is a valid tick");
    }

    @Test
    void toTickRejectsNotAlignedAndOutside() {
        TimeNotAlignedException notAligned = assertThrows(TimeNotAlignedException.class,
                () -> H.toTick(Instant.parse("2026-09-07T06:07:30Z")));
        assertFalse(notAligned.outOfHorizon());
        assertEquals("TIME_NOT_ALIGNED", notAligned.code());
        TimeNotAlignedException before = assertThrows(TimeNotAlignedException.class,
                () -> H.toTick(Instant.parse("2026-09-07T05:45:00Z")));
        assertTrue(before.outOfHorizon());
        assertEquals("TIME_OUT_OF_HORIZON", before.code());
        TimeNotAlignedException after = assertThrows(TimeNotAlignedException.class,
                () -> H.toTick(Instant.parse("2026-09-08T08:30:00Z")));
        assertTrue(after.outOfHorizon());
        assertThrows(TimeNotAlignedException.class, () -> H.toTick(null));
    }

    @Test
    void ceilTick() {
        assertEquals(0, H.ceilTick(START));
        assertEquals(1, H.ceilTick(Instant.parse("2026-09-07T06:07:30Z")));
        assertEquals(1, H.ceilTick(Instant.parse("2026-09-07T06:15:00Z")));
        assertEquals(2, H.ceilTick(Instant.parse("2026-09-07T06:15:01Z")));
        assertEquals(1, H.ceilTick(START.plusMillis(500)));
        // before the start: not clipped
        assertEquals(0, H.ceilTick(Instant.parse("2026-09-07T05:55:00Z")));
        assertEquals(-1, H.ceilTick(Instant.parse("2026-09-07T05:45:00Z")));
        assertEquals(0, H.ceilTick(Instant.parse("2026-09-07T05:50:00Z")));
        assertEquals(-1, H.ceilTick(Instant.parse("2026-09-07T05:35:00Z")));
        // after the end: not clipped
        assertEquals(106, H.ceilTick(Instant.parse("2026-09-08T08:20:00Z")));
    }

    @Test
    void floorTick() {
        assertEquals(0, H.floorTick(START));
        assertEquals(0, H.floorTick(Instant.parse("2026-09-07T06:07:30Z")));
        assertEquals(1, H.floorTick(Instant.parse("2026-09-07T06:15:00Z")));
        assertEquals(1, H.floorTick(Instant.parse("2026-09-07T06:29:59Z")));
        assertEquals(0, H.floorTick(START.plusMillis(500)));
        assertEquals(-1, H.floorTick(Instant.parse("2026-09-07T05:55:00Z")));
        assertEquals(-1, H.floorTick(START.plusMillis(-500)));
        assertEquals(-1, H.floorTick(Instant.parse("2026-09-07T05:45:00Z")));
        assertEquals(105, H.floorTick(Instant.parse("2026-09-08T08:20:00Z")));
    }

    @Test
    void ceilAndFloorSaturateInsteadOfOverflowing() {
        Horizon seconds = new Horizon(START, START.plusSeconds(100), 1);
        assertEquals(Integer.MAX_VALUE, seconds.ceilTick(Instant.parse("2300-01-01T00:00:00Z")));
        assertEquals(Integer.MAX_VALUE, seconds.floorTick(Instant.parse("2300-01-01T00:00:00Z")));
        assertEquals(Integer.MIN_VALUE, seconds.floorTick(Instant.parse("1900-01-01T00:00:00Z")));
    }

    @Test
    void toInstant() {
        assertEquals(START, H.toInstant(0));
        assertEquals(Instant.parse("2026-09-07T07:00:00Z"), H.toInstant(4));
        assertEquals(Instant.parse("2026-09-07T07:15:00Z"), H.toInstant(5));
        assertEquals(H.end(), H.toInstant(105));
        assertEquals(Instant.parse("2026-09-07T05:45:00Z"), H.toInstant(-1));
        for (int tick = 0; tick <= 105; tick++) {
            assertEquals(tick, H.toTick(H.toInstant(tick)));
        }
    }

    @Test
    void ticksAndDuration() {
        assertEquals(3, H.ticks(Duration.ofMinutes(45)));
        assertEquals(0, H.ticks(Duration.ZERO));
        assertEquals(Duration.ofMinutes(45), H.duration(3));
        assertEquals(Duration.ofHours(24), H.duration(96));
        assertThrows(IllegalArgumentException.class, () -> H.ticks(Duration.ofMinutes(10)));
        assertThrows(IllegalArgumentException.class, () -> H.ticks(Duration.ofMillis(900_500)));
        assertThrows(ArithmeticException.class, () -> new Horizon(START, START.plusSeconds(10), 1)
                .ticks(Duration.ofSeconds(Integer.MAX_VALUE + 1L)));
    }

    @Test
    void constructorInvariants() {
        Instant end = Instant.parse("2026-09-08T08:15:00Z");
        assertThrows(IllegalArgumentException.class, () -> new Horizon(null, end, 900));
        assertThrows(IllegalArgumentException.class, () -> new Horizon(START, null, 900));
        assertThrows(IllegalArgumentException.class, () -> new Horizon(START, START, 900), "end must be after start");
        assertThrows(IllegalArgumentException.class, () -> new Horizon(end, START, 900));
        assertThrows(IllegalArgumentException.class, () -> new Horizon(START, end, 0));
        assertThrows(IllegalArgumentException.class, () -> new Horizon(START, end, -60));
        assertThrows(IllegalArgumentException.class, () -> new Horizon(START, end, 7), "must divide 86400");
        assertThrows(IllegalArgumentException.class, () -> new Horizon(START, end, 172_800));
        assertThrows(IllegalArgumentException.class, () -> new Horizon(START, end, 86_401));
        assertThrows(IllegalArgumentException.class,
                () -> new Horizon(START, Instant.parse("2026-09-08T08:10:00Z"), 900), "length not a multiple");
        assertThrows(IllegalArgumentException.class,
                () -> new Horizon(START, START.plusMillis(1500), 1), "sub-second length");
        // every divisor of 86400 listed in the design is accepted
        for (int tick : new int[]{1, 5, 10, 15, 30, 60, 300, 900, 1800, 3600, 86_400}) {
            assertEquals(86_400L / tick, new Horizon(START, START.plusSeconds(86_400), tick).horizonTicks());
        }
    }

    @Test
    void hardCapOnTicks() {
        long cap = Horizon.HARD_MAX_TICKS;
        assertEquals(cap, new Horizon(START, START.plusSeconds(cap), 1).horizonTicks());
        assertThrows(IllegalArgumentException.class, () -> new Horizon(START, START.plusSeconds(cap + 1), 1));
    }

    @Test
    void recordEquality() {
        assertEquals(H, new Horizon(START, Instant.parse("2026-09-08T08:15:00Z"), 900));
        assertEquals(Horizon.MAX_TICK_SECONDS, 86_400);
    }
}
