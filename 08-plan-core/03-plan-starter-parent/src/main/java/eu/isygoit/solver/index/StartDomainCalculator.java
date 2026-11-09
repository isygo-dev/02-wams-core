package eu.isygoit.solver.index;

import java.util.HashMap;
import java.util.Map;

/**
 * Start-domain arithmetic of the index build (design 5.4 step 5, 6.3).
 * <p>
 * A start domain is a sorted list of disjoint INCLUSIVE tick runs {@code [lo0,hi0,lo1,hi1,...]} (even length, empty =
 * empty domain). Availability runs of resources are HALF-OPEN {@code [a,b)}; a task of duration {@code d} may start at
 * {@code s} in a run iff {@code a <= s} and {@code s + d <= b} (non-preemptive, one window; a zero-duration task needs
 * {@code a <= s < b}).
 * <p>
 * The instance part caches, per availability profile class, the start runs for a duration and the clipped result for a
 * window, so tasks sharing a calendar, duration and window reuse one array. Not thread-safe (build time only); the
 * static helpers are pure.
 */
public final class StartDomainCalculator {

    private static final int[] EMPTY = new int[0];

    private final int[][] classRuns;
    private final Map<Long, int[]> startRunsCache = new HashMap<>();
    private final Map<WindowKey, int[]> windowCache = new HashMap<>();

    private record WindowKey(int cls, int duration, int lo, int hi) {
    }

    /**
     * Creates the calculator.
     *
     * @param classRuns half-open availability runs per profile class (calendar + unavailability)
     */
    public StartDomainCalculator(int[][] classRuns) {
        this.classRuns = classRuns;
    }

    /**
     * Allowed start runs of a task of duration {@code d} on a resource of profile {@code cls} within {@code [lo, hi]}.
     *
     * @param cls profile class of the resource, or a negative value for "always available"
     * @param d   duration in ticks on that resource
     * @param lo  earliest start (inclusive)
     * @param hi  latest start (inclusive)
     * @return inclusive runs, shared (do not modify); empty when no start fits
     */
    public int[] allowed(int cls, int d, int lo, int hi) {
        if (hi < lo) {
            return EMPTY;
        }
        if (cls < 0) {
            return new int[]{lo, hi};
        }
        WindowKey key = new WindowKey(cls, d, lo, hi);
        int[] cached = windowCache.get(key);
        if (cached == null) {
            long startKey = (((long) cls) << 32) | (d & 0xFFFFFFFFL);
            int[] starts = startRunsCache.computeIfAbsent(startKey, k -> startRuns(classRuns[cls], d));
            cached = clip(starts, lo, hi);
            windowCache.put(key, cached);
        }
        return cached;
    }

    /**
     * Converts half-open availability runs into inclusive start runs for a duration.
     *
     * @param availability half-open runs {@code [a0,b0,a1,b1,...)}
     * @param d            task duration in ticks
     * @return inclusive start runs {@code [a, b-d]} (for {@code d == 0}: {@code [a, b-1]}) of the runs long enough
     */
    public static int[] startRuns(int[] availability, int d) {
        int len = Math.max(d, 1);
        IntList out = new IntList(availability.length);
        for (int i = 0; i < availability.length; i += 2) {
            int a = availability[i];
            int b = availability[i + 1];
            if (b - a >= len) {
                out.add(a);
                out.add(b - len);
            }
        }
        return out.toArray();
    }

    /**
     * Intersects inclusive runs with the window {@code [lo, hi]} (binary search, O(log n + k)).
     *
     * @return a new array (or {@code runs} itself when nothing is cut)
     */
    public static int[] clip(int[] runs, int lo, int hi) {
        int n = runs.length / 2;
        if (n == 0 || hi < lo) {
            return EMPTY;
        }
        int first = 0;
        int high = n;
        while (first < high) { // first run with run.hi >= lo
            int mid = (first + high) >>> 1;
            if (runs[2 * mid + 1] >= lo) {
                high = mid;
            } else {
                first = mid + 1;
            }
        }
        int last = first;
        while (last < n && runs[2 * last] <= hi) {
            last++;
        }
        if (first >= last) {
            return EMPTY;
        }
        if (first == 0 && last == n && runs[0] >= lo && runs[2 * n - 1] <= hi) {
            return runs;
        }
        int[] out = new int[2 * (last - first)];
        System.arraycopy(runs, 2 * first, out, 0, out.length);
        out[0] = Math.max(out[0], lo);
        out[out.length - 1] = Math.min(out[out.length - 1], hi);
        return out;
    }

    /** Intersection of two inclusive run lists. */
    public static int[] intersect(int[] a, int[] b) {
        if (a.length == 0 || b.length == 0) {
            return EMPTY;
        }
        IntList out = new IntList(Math.min(a.length, b.length));
        int i = 0;
        int j = 0;
        while (i < a.length && j < b.length) {
            int lo = Math.max(a[i], b[j]);
            int hi = Math.min(a[i + 1], b[j + 1]);
            if (lo <= hi) {
                out.add(lo);
                out.add(hi);
            }
            if (a[i + 1] < b[j + 1]) {
                i += 2;
            } else {
                j += 2;
            }
        }
        return out.toArray();
    }

    /** Union of two inclusive run lists (adjacent runs are merged). */
    public static int[] union(int[] a, int[] b) {
        if (a.length == 0) {
            return b;
        }
        if (b.length == 0) {
            return a;
        }
        IntList out = new IntList(a.length + b.length);
        int i = 0;
        int j = 0;
        int curLo = 0;
        int curHi = 0;
        boolean open = false;
        while (i < a.length || j < b.length) {
            int lo;
            int hi;
            if (j >= b.length || (i < a.length && a[i] <= b[j])) {
                lo = a[i];
                hi = a[i + 1];
                i += 2;
            } else {
                lo = b[j];
                hi = b[j + 1];
                j += 2;
            }
            if (!open) {
                curLo = lo;
                curHi = hi;
                open = true;
            } else if ((long) lo <= (long) curHi + 1) {
                curHi = Math.max(curHi, hi);
            } else {
                out.add(curLo);
                out.add(curHi);
                curLo = lo;
                curHi = hi;
            }
        }
        out.add(curLo);
        out.add(curHi);
        return out.toArray();
    }

    /** @return whether {@code v} lies in one of the inclusive runs (binary search) */
    public static boolean contains(int[] runs, int v) {
        int lo = 0;
        int hi = runs.length / 2 - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (runs[2 * mid + 1] < v) {
                lo = mid + 1;
            } else if (runs[2 * mid] > v) {
                hi = mid - 1;
            } else {
                return true;
            }
        }
        return false;
    }

    /** @return the earliest value of a non-empty run list */
    public static int min(int[] runs) {
        return runs[0];
    }

    /** @return the latest value of a non-empty run list */
    public static int max(int[] runs) {
        return runs[runs.length - 1];
    }

    /** @return whether the run list describes exactly the plain interval {@code [lo, hi]} */
    public static boolean isInterval(int[] runs, int lo, int hi) {
        return runs.length == 2 && runs[0] == lo && runs[1] == hi;
    }
}
