package eu.isygoit.solver.cpsat;

import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.params.PeriodSpec;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class BucketGridsTest {

    private static Horizon hourly(String start, int ticks) {
        Instant s = Instant.parse(start);
        return new Horizon(s, s.plusSeconds(ticks * 3600L), 3600);
    }

    private static final PeriodSpec DAY = new PeriodSpec(PeriodSpec.Unit.DAY, null);

    @Test
    void dayGridInUtc() {
        BucketGrid g = BucketGrids.of(hourly("2026-09-07T00:00:00Z", 72), ZoneId.of("UTC"), DAY);
        assertEquals(3, g.size());
        assertArrayEquals(new int[]{0, 24, 48}, g.lo());
        assertArrayEquals(new int[]{24, 48, 72}, g.hi());
        assertEquals("DAY@UTC", g.id());
    }

    @Test
    void springForwardAndFallBackDaysInParis() {
        BucketGrid spring = BucketGrids.of(hourly("2026-03-28T23:00:00Z", 72), "Europe/Paris", DAY);
        assertArrayEquals(new int[]{0, 23, 47, 71}, spring.lo());
        assertEquals(23, spring.hi()[0] - spring.lo()[0]);
        BucketGrid fall = BucketGrids.of(hourly("2026-10-24T22:00:00Z", 72), "Europe/Paris", DAY);
        assertArrayEquals(new int[]{0, 25, 49}, fall.lo());
        assertEquals(25, fall.hi()[0] - fall.lo()[0]);
    }

    @Test
    void weekGridStartsOnMonday() {
        BucketGrid g = BucketGrids.of(hourly("2026-09-09T00:00:00Z", 240), "UTC", new PeriodSpec(PeriodSpec.Unit.WEEK, null));
        assertArrayEquals(new int[]{0, 120}, g.lo());
        assertArrayEquals(new int[]{120, 240}, g.hi());
    }

    @Test
    void ticksGrid() {
        BucketGrid g = BucketGrids.of(hourly("2026-09-07T00:00:00Z", 10), "UTC", new PeriodSpec(PeriodSpec.Unit.TICKS, 4));
        assertArrayEquals(new int[]{0, 4, 8}, g.lo());
        assertArrayEquals(new int[]{4, 8, 10}, g.hi());
        assertEquals("TICKS:4", g.id());
    }

    @Test
    void badOrBlankZoneFallsBackToUtc() {
        Horizon h = hourly("2026-09-07T00:00:00Z", 72);
        assertEquals(BucketGrids.of(h, "UTC", DAY), BucketGrids.of(h, "Not/AZone", DAY));
        assertEquals(BucketGrids.of(h, "UTC", DAY), BucketGrids.of(h, (String) null, DAY));
        assertEquals(ZoneId.of("UTC"), BucketGrids.zoneOf(" "));
    }

    @Test
    void gridsOfDifferentZonesAreDifferentCacheKeys() {
        Horizon h = hourly("2026-09-07T00:00:00Z", 72);
        assertNotEquals(BucketGrids.of(h, "UTC", DAY), BucketGrids.of(h, "Europe/Paris", DAY));
    }

    @Test
    void problemOverloadReadsHorizonAndTimezone() {
        Horizon h = hourly("2026-03-28T23:00:00Z", 72);
        PlanningProblem p = new PlanningProblem(1L, "x", "Europe/Paris", h, null, null, null, List.of(), List.of(), List.of(),
                List.of(), null, null);
        assertArrayEquals(new int[]{0, 23, 47, 71}, BucketGrids.of(p, DAY).lo());
    }
}
