package eu.isygoit.solver.timefold;

import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.Task;
import eu.isygoit.solver.CompiledProblem;
import eu.isygoit.solver.index.CandidateIndex;
import eu.isygoit.util.ScaledMath;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Mutable workspace filled by the Timefold contributors (design 5.6): option-level data only, no Timefold stream code.
 * <p>
 * Created by {@link TimefoldSolverEngine} once per solve from the BASE {@link SlotOptions} table; handed to every
 * {@link TimefoldConstraintContributor} / {@link TimefoldObjectiveContributor} on the building thread (single-threaded,
 * NOT thread-safe); then {@link #freeze()} produces the immutable table the solver uses. Option ids handed to
 * contributors are the ids of the BASE table ({@link #options()}); {@link #freeze()} drops forbidden options and renumbers.
 * <p>
 * What contributors can express:
 * <ul>
 *   <li>{@link #forbid(int, String)}: the option can never be chosen (hard pruning);</li>
 *   <li>{@link #addPenalty(int, long)}: a scaled soft cost of choosing the option (weights already applied);</li>
 *   <li>{@link #addSelectionReward(int, long)}: a scaled reward when an OPTIONAL task is selected;</li>
 *   <li>{@link #lowerCapacity(int, int)}: lowers the capacity of the (resource, slot) bucket of an option.</li>
 * </ul>
 * All sums are guarded to {@code 2^53} ({@link ScaledMath#guard}) so scores stay exact in JSON doubles and JS. Units: ticks
 * for time, scaled longs for money-like values.
 */
public final class TimefoldBuildContext {

    private final CompiledProblem compiled;
    private final CandidateIndex index;
    private final SlotOptions base;
    private final long[] penalty;
    private final long[] reward;
    private final int[] capacity;
    private final boolean[] forbidden;
    private String[] forbiddenBy;
    private int forbiddenCount;

    /**
     * @param compiled the compiled problem
     * @param base     the base option table of {@code compiled.index()} (see {@link SlotOptionsBuilder})
     */
    public TimefoldBuildContext(CompiledProblem compiled, SlotOptions base) {
        this.compiled = compiled;
        this.index = compiled.index();
        this.base = base;
        int options = base.optionCount();
        this.penalty = new long[options];
        this.reward = new long[base.taskCount()];
        this.capacity = new int[options];
        for (int o = 0; o < options; o++) {
            capacity[o] = base.capacity(o);
        }
        this.forbidden = new boolean[options];
    }

    /** @return the candidate index */
    public CandidateIndex index() {
        return index;
    }

    /** @return the normalized problem (resources/tasks in index order: {@code problem().resources().get(r)}) */
    public PlanningProblem problem() {
        return compiled.problem();
    }

    /** @return the BASE option table (neutral pruning applied); option ids used by every method of this context */
    public SlotOptions options() {
        return base;
    }

    /** @return the resource record of resource index {@code r} */
    public Resource resource(int r) {
        return compiled.problem().resources().get(r);
    }

    /** @return the task record of task index {@code t} */
    public Task task(int t) {
        return compiled.problem().tasks().get(t);
    }

    /** @return the live soft penalty array {@code [option]} (prefer {@link #addPenalty}); scaled */
    public long[] optionPenalty() {
        return penalty;
    }

    /** @return the live selection reward array {@code [task]} (prefer {@link #addSelectionReward}); scaled */
    public long[] selectionReward() {
        return reward;
    }

    /** @return the live effective capacity array {@code [option]} of the option's (resource, slot) (prefer {@link #lowerCapacity}) */
    public int[] optionCapacity() {
        return capacity;
    }

    /**
     * Forbids an option (idempotent: the first constraint id is kept for diagnostics).
     *
     * @param option       a BASE option id
     * @param constraintId the constraint that forbids it
     */
    public void forbid(int option, String constraintId) {
        if (!forbidden[option]) {
            forbidden[option] = true;
            forbiddenCount++;
            if (forbiddenBy == null) {
                forbiddenBy = new String[forbidden.length];
            }
            forbiddenBy[option] = constraintId;
        }
    }

    /** @return {@code true} when the BASE option was forbidden */
    public boolean isForbidden(int option) {
        return forbidden[option];
    }

    /**
     * Adds a scaled soft cost to an option.
     *
     * @param option a BASE option id
     * @param delta  the scaled cost (weight already applied), usually {@code >= 0}
     * @throws eu.isygoit.exception.ObjectiveRangeException when the sum leaves the exact range
     */
    public void addPenalty(int option, long delta) {
        penalty[option] = ScaledMath.guard(ScaledMath.addExact(penalty[option], delta), "option penalty");
    }

    /**
     * Adds a scaled reward for selecting an optional task.
     *
     * @param task  a task index
     * @param delta the scaled reward (weight already applied), usually {@code >= 0}
     * @throws eu.isygoit.exception.ObjectiveRangeException when the sum leaves the exact range
     */
    public void addSelectionReward(int task, long delta) {
        reward[task] = ScaledMath.guard(ScaledMath.addExact(reward[task], delta), "selection reward");
    }

    /**
     * Lowers the effective capacity of the (resource, slot) bucket of an option to {@code min(current, capacity)}; when
     * the option's demand no longer fits it is forbidden by the caller. Every option of the same (resource, slot) must be
     * lowered by the caller consistently (iterate all options and test the resource/slot, not one option).
     *
     * @param option   a BASE option id
     * @param newLimit the new upper bound of the capacity ({@code >= 0})
     */
    public void lowerCapacity(int option, int newLimit) {
        capacity[option] = Math.min(capacity[option], Math.max(0, newLimit));
    }

    /**
     * @param scope a task scope ({@code all|task|taskType|tag}); {@code null} = all
     * @return {@code null} when the scope selects every task, else a mask {@code [task]}
     */
    public boolean[] taskMask(ConstraintScope scope) {
        if (scope == null || ConstraintScope.ALL.equals(scope.kind())) {
            return null;
        }
        boolean[] mask = new boolean[index.taskCount()];
        for (int t : index.resolveTasks(scope)) {
            mask[t] = true;
        }
        return mask;
    }

    /**
     * @param scope a resource scope ({@code all|resource|resourceId|resourceType|tag}); {@code null} = all
     * @return {@code null} when the scope selects every resource, else a mask {@code [resource]}
     */
    public boolean[] resourceMask(ConstraintScope scope) {
        if (scope == null || ConstraintScope.ALL.equals(scope.kind())) {
            return null;
        }
        boolean[] mask = new boolean[index.resourceCount()];
        for (int r : index.resolveResources(scope)) {
            mask[r] = true;
        }
        return mask;
    }

    /**
     * @param scope an objective scope
     * @return {@code true} when the scope kind selects RESOURCES ({@code resource}, {@code resourceId}, {@code resourceType})
     */
    public static boolean isResourceScope(ConstraintScope scope) {
        if (scope == null || scope.kind() == null) {
            return false;
        }
        String kind = scope.kind().toLowerCase(Locale.ROOT);
        return kind.equals("resource") || kind.equals("resourceid") || kind.equals("resourcetype");
    }

    /**
     * @param task a task index
     * @return the ids of the constraints that forbade at least one option of the task (diagnostics, may be empty)
     */
    public Set<String> forbiddingConstraints(int task) {
        Set<String> ids = new LinkedHashSet<>();
        if (forbiddenBy != null) {
            for (int o = base.taskOptionStart(task); o < base.taskOptionEnd(task); o++) {
                if (forbiddenBy[o] != null) {
                    ids.add(forbiddenBy[o]);
                }
            }
        }
        return ids;
    }

    /** @return number of options forbidden so far */
    public int forbiddenCount() {
        return forbiddenCount;
    }

    /**
     * Produces the immutable table used by the solver: forbidden options and options whose demand exceeds their capacity
     * are dropped, the others keep their order and receive their penalty and capacity; the task rewards are copied.
     *
     * @return the frozen, compacted table (same task indexing as the base table)
     */
    public SlotOptions freeze() {
        int options = base.optionCount();
        int taskCount = base.taskCount();
        int[] keep = new int[options];
        int kept = 0;
        int[] taskStart = new int[taskCount + 1];
        for (int t = 0; t < taskCount; t++) {
            taskStart[t] = kept;
            for (int o = base.taskOptionStart(t); o < base.taskOptionEnd(t); o++) {
                if (!forbidden[o] && base.demand(o) <= capacity[o]) {
                    keep[kept++] = o;
                }
            }
        }
        taskStart[taskCount] = kept;
        int[] candidate = new int[kept];
        int[] resource = new int[kept];
        int[] slot = new int[kept];
        int[] start = new int[kept];
        int[] end = new int[kept];
        int[] demand = new int[kept];
        int[] cap = new int[kept];
        long[] pen = new long[kept];
        for (int i = 0; i < kept; i++) {
            int o = keep[i];
            candidate[i] = base.candidate(o);
            resource[i] = base.resource(o);
            slot[i] = base.slot(o);
            start[i] = base.start(o);
            end[i] = base.end(o);
            demand[i] = base.demand(o);
            cap[i] = capacity[o];
            pen[i] = penalty[o];
        }
        return new SlotOptions(taskCount, base.slotCount(), taskStart, candidate, resource, slot, start, end, demand, cap,
                pen, Arrays.copyOf(reward, taskCount));
    }
}
