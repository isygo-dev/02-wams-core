package eu.isygoit.plan.engine.timefold;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

public class PlanConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[]{
                hardEligibility(factory),
                capacity(factory),
                softPenalty(factory),
                objectiveCost(factory),
                objectiveReward(factory)
        };
    }

    private Constraint hardEligibility(ConstraintFactory factory) {
        return factory.forEachIncludingUnassigned(TimefoldTaskAssignment.class)
                .filter(assignment -> !assignment.isHardAllowed())
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Hard assignment eligibility");
    }

    private Constraint capacity(ConstraintFactory factory) {
        return factory.forEach(TimefoldTaskAssignment.class)
                .groupBy(
                        TimefoldTaskAssignment::getResource,
                        TimefoldTaskAssignment::getSlot,
                        TimefoldTaskAssignment::getCapacity,
                        ConstraintCollectors.count())
                .filter((resource, slot, capacity, count) -> count > capacity)
                .penalize(HardSoftScore.ONE_HARD, (resource, slot, capacity, count) -> count - capacity)
                .asConstraint("Resource capacity");
    }

    private Constraint softPenalty(ConstraintFactory factory) {
        return factory.forEach(TimefoldTaskAssignment.class)
                .filter(assignment -> assignment.getSoftPenalty() > 0)
                .penalize(HardSoftScore.ONE_SOFT, TimefoldTaskAssignment::getSoftPenalty)
                .asConstraint("Soft constraint penalty");
    }

    private Constraint objectiveCost(ConstraintFactory factory) {
        return factory.forEach(TimefoldTaskAssignment.class)
                .filter(assignment -> assignment.getObjectiveCost() > 0L)
                .penalize(HardSoftScore.ONE_SOFT,
                        assignment -> Math.toIntExact(assignment.getObjectiveCost()))
                .asConstraint("Planning objectives");
    }

    private Constraint objectiveReward(ConstraintFactory factory) {
        return factory.forEach(TimefoldTaskAssignment.class)
                .filter(assignment -> assignment.getObjectiveCost() < 0L)
                .reward(HardSoftScore.ONE_SOFT,
                        assignment -> Math.toIntExact(-assignment.getObjectiveCost()))
                .asConstraint("Planning objective rewards");
    }
}
