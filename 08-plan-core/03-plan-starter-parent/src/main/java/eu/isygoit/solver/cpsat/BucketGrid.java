package eu.isygoit.solver.cpsat;

import java.util.Arrays;

/**
 * A tiling of the horizon into consecutive periods (DAY / WEEK in {@code meta.timezone}, or fixed TICKS): bucket
 * {@code k} covers the half-open tick range {@code [lo[k], hi[k])}.
 * <p>
 * Created by the grid factory ({@code BucketGrids}, WP4b) and cached by {@code CpModelContext.buckets(PeriodSpec)};
 * consumed by windowed families. Immutable: the arrays are defensively copied on construction and MUST NOT be modified by
 * readers. Units: ticks.
 *
 * @param lo inclusive start tick of each bucket, ascending
 * @param hi exclusive end tick of each bucket ({@code hi.length == lo.length})
 * @param id grid identifier used as cache key (for example {@code DAY@Europe/Paris})
 */
public record BucketGrid(int[] lo, int[] hi, String id) {

    public BucketGrid {
        if (lo == null || hi == null || lo.length != hi.length) {
            throw new IllegalArgumentException("BucketGrid lo/hi must be non-null arrays of equal length");
        }
        lo = lo.clone();
        hi = hi.clone();
    }

    /** @return the number of buckets */
    public int size() {
        return lo.length;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BucketGrid g
                && Arrays.equals(lo, g.lo)
                && Arrays.equals(hi, g.hi)
                && java.util.Objects.equals(id, g.id);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * Arrays.hashCode(lo) + Arrays.hashCode(hi)) + java.util.Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "BucketGrid[id=" + id + ", buckets=" + lo.length + "]";
    }
}
