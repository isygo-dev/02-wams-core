package eu.isygoit.solver.cpsat.explain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeletionFilterTest {

    /** Infeasible iff the subset contains every item of {@code conflict}. */
    private static DeletionFilter.Oracle conflict(int... conflict) {
        return subset -> {
            for (int needed : conflict) {
                boolean found = false;
                for (int item : subset) {
                    if (item == needed) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return DeletionFilter.Verdict.satisfiable();
                }
            }
            return DeletionFilter.Verdict.infeasible(null);
        };
    }

    private static DeletionFilter filter(int max) {
        return new DeletionFilter(max, Long.MAX_VALUE, System::nanoTime);
    }

    @Test
    void shrinksToTheMinimalConflictAndProvesMinimality() {
        DeletionFilter.Result r = filter(30).shrink(new int[]{0, 1, 2, 3, 4}, conflict(1, 3));
        int[] core = r.core().clone();
        Arrays.sort(core);
        assertArrayEquals(new int[]{1, 3}, core);
        assertTrue(r.minimal());
        assertEquals(5, r.oracleCalls());
    }

    @Test
    void usesTheSmallerSubsetReturnedByTheOracle() {
        // infeasible exactly when item 2 is present, and the oracle reports {2} as the reason
        DeletionFilter.Oracle oracle = subset -> Arrays.stream(subset).anyMatch(i -> i == 2)
                ? DeletionFilter.Verdict.infeasible(new int[]{2}) : DeletionFilter.Verdict.satisfiable();
        DeletionFilter.Result r = filter(30).shrink(new int[]{0, 1, 2, 3}, oracle);
        // after removing item 0 the oracle already proves {2} infeasible: items 1 and 3 are skipped as removed
        assertArrayEquals(new int[]{2}, r.core());
    }

    @Test
    void coreLargerThanTheItemCapIsNotMinimal() {
        int[] core = new int[40];
        for (int i = 0; i < core.length; i++) {
            core[i] = i;
        }
        DeletionFilter.Result r = filter(30).shrink(core, conflict(5, 6));
        assertFalse(r.minimal());
        assertEquals(30, r.oracleCalls());
        assertTrue(r.core().length < 40);
    }

    @Test
    void inconclusiveAnswersKeepTheItemAndForbidMinimality() {
        DeletionFilter.Oracle oracle = subset -> DeletionFilter.Verdict.unknown();
        DeletionFilter.Result r = filter(30).shrink(new int[]{7, 8}, oracle);
        assertArrayEquals(new int[]{7, 8}, r.core());
        assertFalse(r.minimal());
    }

    @Test
    void deadlineStopsTheLoop() {
        AtomicLong clock = new AtomicLong();
        DeletionFilter f = new DeletionFilter(30, 10, clock::get);
        DeletionFilter.Oracle oracle = subset -> {
            clock.addAndGet(6);
            return DeletionFilter.Verdict.satisfiable();
        };
        DeletionFilter.Result r = f.shrink(new int[]{1, 2, 3, 4}, oracle);
        assertEquals(2, r.oracleCalls(), "clock 0 -> 6 -> 12 >= deadline 10");
        assertFalse(r.minimal());
        assertEquals(4, r.core().length);
    }

    @Test
    void singleItemCoreIsMinimalWhenTheEmptySetIsSatisfiable() {
        DeletionFilter.Result r = filter(30).shrink(new int[]{4}, conflict(4));
        assertArrayEquals(new int[]{4}, r.core());
        assertTrue(r.minimal());
    }
}
