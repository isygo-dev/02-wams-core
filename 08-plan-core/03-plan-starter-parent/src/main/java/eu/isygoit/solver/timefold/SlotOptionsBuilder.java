package eu.isygoit.solver.timefold;

import eu.isygoit.exception.InvalidRequestException;
import eu.isygoit.solver.index.CandidateIndex;

import java.util.Arrays;

/**
 * Derives the {@link SlotOptions} table (one option per surviving (candidate, slot) pair) from the {@link CandidateIndex}
 * (design 5.6).
 * <p>
 * Pure function of the index, no Spring, no Timefold types; called once per solve on the building thread. Complexity
 * {@code O(sum over tasks of candidates x slots of the task)} with a binary search for the calendar check. The result is
 * the BASE table: only NEUTRAL pruning is applied here (the same facts the CP-SAT model uses), the contributors forbid
 * further options through {@link TimefoldBuildContext}.
 * <p>
 * An option (task t, candidate c on resource r, slot s) survives when, in ticks:
 * <ul>
 *   <li>{@code slotEnd(s) - slotStart(s) >= duration(c)}: the task interval {@code [slotStart, slotStart + duration)} ends
 *       inside the slot (the slot start is the task start);</li>
 *   <li>the start lies in {@code [estStart(t), lstStart(t)]} and is {@code >= release(t)}, the end is
 *       {@code <= latestEnd(t)} and {@code <= horizonTicks}; a pinned start is respected;</li>
 *   <li>the per-candidate start-run override of the index (inclusive runs), when present, contains the start;</li>
 *   <li>the whole interval lies inside ONE available run of the resource (calendar + unavailability) when the resource has
 *       runs.</li>
 * </ul>
 * Tasks without any allowed slot (free start) and tasks with more than one resource are not supported by Timefold and
 * make the build fail with {@code ENGINE_UNSUPPORTED_FEATURE}; slots used by at least one option must be pairwise
 * disjoint, otherwise the build fails with {@code OVERLAPPING_SLOTS} (the per-slot capacity formulation would be wrong).
 * These are normally rejected earlier by the {@code ProblemVerifier}; the engine re-checks defensively and never ignores
 * them silently.
 */
public final class SlotOptionsBuilder {

    /** Cancellation/time checkpoint interval of the build loops. */
    private static final int CHECKPOINT_INTERVAL = 1024;

    private SlotOptionsBuilder() {
    }

    /**
     * Builds the base table.
     *
     * @param index      the candidate index of the compiled problem
     * @param checkpoint called every 1024 iterations (may throw {@code SolveCancelledException}); may be {@code null}
     * @return the base table: zero penalties/rewards, capacity = resource capacity
     * @throws InvalidRequestException {@code ENGINE_UNSUPPORTED_FEATURE} (free start / more than one requirement) or
     *                                 {@code OVERLAPPING_SLOTS}
     */
    public static SlotOptions build(CandidateIndex index, Runnable checkpoint) {
        int taskCount = index.taskCount();
        int slotCount = index.slotCount();
        int[] taskOptionStart = new int[taskCount + 1];
        IntBuffer optCandidate = new IntBuffer();
        IntBuffer optResource = new IntBuffer();
        IntBuffer optSlot = new IntBuffer();
        IntBuffer optStart = new IntBuffer();
        IntBuffer optEnd = new IntBuffer();
        IntBuffer optDemand = new IntBuffer();
        IntBuffer optCapacity = new IntBuffer();
        boolean[] slotUsed = new boolean[slotCount];
        long iterations = 0;

        for (int t = 0; t < taskCount; t++) {
            taskOptionStart[t] = optCandidate.size();
            requireSingleRequirement(index, t);
            int slotFrom = index.taskSlotStart[t];
            int slotTo = index.taskSlotStart[t + 1];
            if (slotFrom >= slotTo) {
                throw new InvalidRequestException("ENGINE_UNSUPPORTED_FEATURE",
                        "Engine 'timefold' supports slot mode only: task '" + index.taskId(t)
                                + "' has no start slot (FREE_START)");
            }
            int release = Math.max(index.release(t), index.estStart(t));
            int lastStart = index.lstStart(t);
            int latestEnd = Math.min(index.latestEnd(t), index.horizonTicks());
            int pinStart = index.pinStart(t);
            for (int c = index.candidateBegin(t); c < index.candidateEnd(t); c++) {
                int resource = index.candidateResource(c);
                int duration = index.candidateDuration(c);
                int amount = index.candidateAmount(c);
                int resFrom = runFrom(index.resRunStart, resource);
                int resTo = runTo(index.resRunStart, resource);
                int ovFrom = runFrom(index.candStartRunStart, c);
                int ovTo = runTo(index.candStartRunStart, c);
                for (int k = slotFrom; k < slotTo; k++) {
                    if ((++iterations % CHECKPOINT_INTERVAL) == 0 && checkpoint != null) {
                        checkpoint.run();
                    }
                    int slot = index.taskSlots[k];
                    int start = index.slotStart[slot];
                    long end = (long) start + duration;
                    if (end > index.slotEnd[slot] || end > latestEnd
                            || start < release || start > lastStart
                            || (pinStart >= 0 && start != pinStart)) {
                        continue;
                    }
                    if (ovFrom < ovTo && !startInInclusiveRuns(index.candStartRuns, ovFrom, ovTo, start)) {
                        continue;
                    }
                    if (resFrom < resTo && !fitsAvailableRun(index.resRuns, resFrom, resTo, start, (int) end)) {
                        continue;
                    }
                    optCandidate.add(c);
                    optResource.add(resource);
                    optSlot.add(slot);
                    optStart.add(start);
                    optEnd.add((int) end);
                    optDemand.add(duration == 0 ? 0 : amount);
                    optCapacity.add(index.capacity(resource));
                    slotUsed[slot] = true;
                }
            }
        }
        taskOptionStart[taskCount] = optCandidate.size();
        requireDisjointUsedSlots(index, slotUsed);
        int optionCount = optCandidate.size();
        return new SlotOptions(taskCount, slotCount, taskOptionStart, optCandidate.toArray(), optResource.toArray(),
                optSlot.toArray(), optStart.toArray(), optEnd.toArray(), optDemand.toArray(), optCapacity.toArray(),
                new long[optionCount], new long[taskCount]);
    }

    private static void requireSingleRequirement(CandidateIndex index, int t) {
        int from = index.taskReqStart[t];
        int to = index.taskReqStart[t + 1];
        if (to - from != 1 || index.reqCount[from] != 1) {
            throw new InvalidRequestException("ENGINE_UNSUPPORTED_FEATURE",
                    "Engine 'timefold' supports exactly one resource per task (MULTI_RESOURCE): task '"
                            + index.taskId(t) + "'");
        }
    }

    /** Slots referenced by at least one option must not overlap (half-open intervals; touching is fine). */
    private static void requireDisjointUsedSlots(CandidateIndex index, boolean[] slotUsed) {
        int used = 0;
        for (boolean b : slotUsed) {
            if (b) {
                used++;
            }
        }
        if (used < 2) {
            return;
        }
        long[] keyed = new long[used];
        int n = 0;
        for (int s = 0; s < slotUsed.length; s++) {
            if (slotUsed[s]) {
                keyed[n++] = (((long) index.slotStart[s]) << 32) | (long) s;
            }
        }
        Arrays.sort(keyed);
        int previous = (int) (keyed[0] & 0xFFFFFFFFL);
        for (int i = 1; i < keyed.length; i++) {
            int current = (int) (keyed[i] & 0xFFFFFFFFL);
            if (index.slotEnd[previous] > index.slotStart[current]) {
                throw new InvalidRequestException("OVERLAPPING_SLOTS",
                        "Engine 'timefold' needs pairwise disjoint slots: '" + index.slotId(previous)
                                + "' overlaps '" + index.slotId(current) + "'");
            }
            if (index.slotEnd[current] > index.slotEnd[previous]) {
                previous = current;
            }
        }
    }

    private static int runFrom(int[] csr, int i) {
        return csr == null || i + 1 >= csr.length ? 0 : csr[i];
    }

    private static int runTo(int[] csr, int i) {
        return csr == null || i + 1 >= csr.length ? 0 : csr[i + 1];
    }

    /** Runs are flat pairs {@code [lo0,hi0,lo1,hi1,...)} in {@code runs[from, to)}; half-open {@code [lo,hi)}. */
    static boolean fitsAvailableRun(int[] runs, int from, int to, int start, int end) {
        int found = lastRunStartingAtOrBefore(runs, from, to, start);
        return found >= 0 && end <= runs[from + 2 * found + 1];
    }

    /** Runs are flat pairs in {@code runs[from, to)}; inclusive {@code [lo,hi]}. */
    static boolean startInInclusiveRuns(int[] runs, int from, int to, int start) {
        int found = lastRunStartingAtOrBefore(runs, from, to, start);
        return found >= 0 && start <= runs[from + 2 * found + 1];
    }

    private static int lastRunStartingAtOrBefore(int[] runs, int from, int to, int start) {
        int lo = 0;
        int hi = (to - from) / 2 - 1;
        int found = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (runs[from + 2 * mid] <= start) {
                found = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return found;
    }

    /** Minimal growable int array (no boxing). */
    private static final class IntBuffer {
        private int[] data = new int[64];
        private int size;

        void add(int value) {
            if (size == data.length) {
                data = Arrays.copyOf(data, size * 2);
            }
            data[size++] = value;
        }

        int size() {
            return size;
        }

        int[] toArray() {
            return Arrays.copyOf(data, size);
        }
    }
}
