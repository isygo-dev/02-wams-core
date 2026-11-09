package eu.isygoit.solver.timefold;

import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

/**
 * The four score constraints of the Timefold subset (design 5.6). Everything else (eligibility, windows, availability,
 * pins, soft eligibility, costs) is already folded into the OPTION table by the contributors, so no stream here looks at
 * any domain concept beyond the chosen option.
 * <ul>
 *   <li>hard {@link #REQUIRED}: a required task without option, 1 per task;</li>
 *   <li>hard {@link #CAPACITY}: per (resource, slot) bucket, sum of demands above the bucket capacity;</li>
 *   <li>soft {@link #PENALTY}: scaled penalty of the chosen option;</li>
 *   <li>soft {@link #REWARD}: scaled reward of a selected task (maximisation objectives).</li>
 * </ul>
 * Instantiated reflectively by Timefold (no state, public no-arg constructor); thread-safe.
 */
public class PlanConstraintProvider implements ConstraintProvider {

    /** Constraint name: required task not scheduled. */
    public static final String REQUIRED = "Required task scheduled";
    /** Constraint name: per-slot resource capacity. */
    public static final String CAPACITY = "Resource capacity per slot";
    /** Constraint name: option penalties. */
    public static final String PENALTY = "Option penalty";
    /** Constraint name: selection rewards. */
    public static final String REWARD = "Selection reward";

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[]{
                requiredTaskScheduled(factory),
                resourceCapacity(factory),
                optionPenalty(factory),
                selectionReward(factory)
        };
    }

    private Constraint requiredTaskScheduled(ConstraintFactory factory) {
        return factory.forEachIncludingUnassigned(TimefoldTask.class)
                .filter(task -> task.isRequired() && task.getOption() == null)
                .penalize(HardSoftLongScore.ONE_HARD)
                .asConstraint(REQUIRED);
    }

    // TIMEFOLD-VERIFY: groupBy(key, key, sum) -> TriConstraintStream, penalize with ToIntTriFunction (signatures checked with javap).
    private Constraint resourceCapacity(ConstraintFactory factory) {
        return factory.forEach(TimefoldTask.class)
                .filter(task -> task.getDemand() > 0)
                .groupBy(TimefoldTask::getCapacityKey, TimefoldTask::getSlotCapacity,
                        ConstraintCollectors.sum(TimefoldTask::getDemand))
                .filter((key, capacity, demand) -> demand > capacity)
                .penalize(HardSoftLongScore.ONE_HARD, (key, capacity, demand) -> demand - capacity)
                .asConstraint(CAPACITY);
    }

    private Constraint optionPenalty(ConstraintFactory factory) {
        return factory.forEach(TimefoldTask.class)
                .filter(task -> task.getPenalty() > 0L)
                .penalizeLong(HardSoftLongScore.ONE_SOFT, TimefoldTask::getPenalty)
                .asConstraint(PENALTY);
    }

    private Constraint selectionReward(ConstraintFactory factory) {
        return factory.forEach(TimefoldTask.class)
                .filter(task -> task.getReward() > 0L)
                .rewardLong(HardSoftLongScore.ONE_SOFT, TimefoldTask::getReward)
                .asConstraint(REWARD);
    }
}
