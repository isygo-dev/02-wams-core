package eu.isygoit.solver.spi;

import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.params.PeriodSpec;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PeriodBucketsTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final ZoneId UTC = ZoneId.of("UTC");

    private static Horizon hourly(String start, int ticks) {
        Instant s = Instant.parse(start);
        return new Horizon(s, s.plusSeconds(ticks * 3600L), 3600);
    }

    private static PeriodSpec day() {
        return new PeriodSpec(PeriodSpec.Unit.DAY, null);
    }

    @Test
    void ticksBucketsTileTheHorizonWithAShorterLastBucket() {
        int[][] t = PeriodBuckets.tile(hourly("2026-09-07T00:00:00Z", 10), UTC, new PeriodSpec(PeriodSpec.Unit.TICKS, 4));
        assertArrayEquals(new int[]{0, 4, 8}, t[0]);
        assertArrayEquals(new int[]{4, 8, 10}, t[1]);
    }

    @Test
    void ticksNeedAPositiveWidth() {
        assertThrows(IllegalArgumentException.class,
                () -> PeriodBuckets.tile(hourly("2026-09-07T00:00:00Z", 10), UTC, new PeriodSpec(PeriodSpec.Unit.TICKS, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> PeriodBuckets.tile(hourly("2026-09-07T00:00:00Z", 10), UTC, new PeriodSpec(PeriodSpec.Unit.TICKS, null)));
        assertThrows(IllegalArgumentException.class,
                () -> PeriodBuckets.tile(hourly("2026-09-07T00:00:00Z", 10), UTC, new PeriodSpec(null, 3)));
    }

    @Test
    void utcDaysAreTwentyFourHourBuckets() {
        int[][] t = PeriodBuckets.tile(hourly("2026-09-07T00:00:00Z", 72), UTC, day());
        assertArrayEquals(new int[]{0, 24, 48}, t[0]);
        assertArrayEquals(new int[]{24, 48, 72}, t[1]);
    }

    @Test
    void aHorizonStartingMidDayHasPartialFirstAndLastBuckets() {
        int[][] t = PeriodBuckets.tile(hourly("2026-09-07T06:00:00Z", 48), UTC, day());
        assertArrayEquals(new int[]{0, 18, 42}, t[0]);
        assertArrayEquals(new int[]{18, 42, 48}, t[1]);
    }

    @Test
    void weeksStartOnMonday() {
        // Wednesday 2026-09-09 00:00Z, ten days: next Monday is 2026-09-14 = tick 120
        int[][] t = PeriodBuckets.tile(hourly("2026-09-09T00:00:00Z", 240), UTC, new PeriodSpec(PeriodSpec.Unit.WEEK, null));
        assertArrayEquals(new int[]{0, 120}, t[0]);
        assertArrayEquals(new int[]{120, 240}, t[1]);
    }

    @Test
    void springForwardDayHasTwentyThreeHours() {
        // Paris 2026-03-29: 02:00 -> 03:00. Horizon starts at local midnight (2026-03-28T23:00Z).
        int[][] t = PeriodBuckets.tile(hourly("2026-03-28T23:00:00Z", 72), PARIS, day());
        assertArrayEquals(new int[]{0, 23, 47, 71}, t[0]);
        assertArrayEquals(new int[]{23, 47, 71, 72}, t[1]);
        // tick 22 = 23:00 CEST on the 29th, tick 23 = 00:00 CEST on the 30th
        assertEquals(0, PeriodBuckets.bucketOf(t[0], t[1], 22));
        assertEquals(1, PeriodBuckets.bucketOf(t[0], t[1], 23));
    }

    @Test
    void fallBackDayHasTwentyFiveHours() {
        // Paris 2026-10-25: 03:00 -> 02:00. Horizon starts at local midnight (2026-10-24T22:00Z).
        int[][] t = PeriodBuckets.tile(hourly("2026-10-24T22:00:00Z", 72), PARIS, day());
        assertArrayEquals(new int[]{0, 25, 49}, t[0]);
        assertArrayEquals(new int[]{25, 49, 72}, t[1]);
        assertEquals(0, PeriodBuckets.bucketOf(t[0], t[1], 24));
        assertEquals(1, PeriodBuckets.bucketOf(t[0], t[1], 25));
    }

    @Test
    void coarseTicksStillAlignDayBoundaries() {
        // one tick = one day (86400 s), seven days
        Horizon h = new Horizon(Instant.parse("2026-09-07T00:00:00Z"), Instant.parse("2026-09-14T00:00:00Z"), 86400);
        int[][] t = PeriodBuckets.tile(h, UTC, day());
        assertEquals(7, t[0].length);
        int[][] w = PeriodBuckets.tile(h, UTC, new PeriodSpec(PeriodSpec.Unit.WEEK, null));
        assertArrayEquals(new int[]{0}, w[0]);
        assertArrayEquals(new int[]{7}, w[1]);
    }

    @Test
    void zoneWithOffsetShiftsTheBoundaryToTheNextTick() {
        // Kolkata is UTC+05:30: local midnight is 18:30Z, hourly ticks -> first tick at or after it is 19:00Z
        Horizon h = hourly("2026-09-07T00:00:00Z", 48);
        int[][] t = PeriodBuckets.tile(h, ZoneId.of("Asia/Kolkata"), day());
        assertArrayEquals(new int[]{0, 19, 43}, t[0]);
    }

    @Test
    void bucketOfAndRange() {
        int[] lo = {0, 24, 48};
        int[] hi = {24, 48, 72};
        assertEquals(0, PeriodBuckets.bucketOf(lo, hi, 0));
        assertEquals(0, PeriodBuckets.bucketOf(lo, hi, 23));
        assertEquals(1, PeriodBuckets.bucketOf(lo, hi, 24));
        assertEquals(2, PeriodBuckets.bucketOf(lo, hi, 71));
        assertEquals(-1, PeriodBuckets.bucketOf(lo, hi, 72));
        assertEquals(-1, PeriodBuckets.bucketOf(lo, hi, -1));
        assertArrayEquals(new int[]{0, 2}, PeriodBuckets.bucketRange(lo, hi, 5, 60));
        assertArrayEquals(new int[]{1, 1}, PeriodBuckets.bucketRange(lo, hi, 30, 30));
        assertArrayEquals(new int[]{2, 2}, PeriodBuckets.bucketRange(lo, hi, 60, 500));
        assertNull(PeriodBuckets.bucketRange(lo, hi, 9, 8));
        assertNull(PeriodBuckets.bucketRange(lo, hi, 80, 90));
    }

    @Test
    void idsDifferByUnitAndZone() {
        assertEquals("TICKS:12", PeriodBuckets.id(new PeriodSpec(PeriodSpec.Unit.TICKS, 12), PARIS));
        assertEquals("DAY@Europe/Paris", PeriodBuckets.id(day(), PARIS));
        assertEquals("WEEK@UTC", PeriodBuckets.id(new PeriodSpec(PeriodSpec.Unit.WEEK, null), null));
    }
}
