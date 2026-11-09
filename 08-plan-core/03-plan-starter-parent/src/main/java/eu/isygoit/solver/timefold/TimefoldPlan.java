package eu.isygoit.solver.timefold;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactProperty;
import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;

import java.util.List;

/**
 * Timefold planning solution of the subset (design 5.6): the shared immutable {@link SlotOptions} table as the only
 * problem fact, one {@link TimefoldTask} entity per task that has at least one option, and a 64-bit
 * {@link HardSoftLongScore} (hard: unassigned required tasks and capacity overflow; soft: penalties minus rewards).
 * <p>
 * Created by {@link TimefoldSolverEngine} per solve; owned by the solver thread during the solve. Not thread-safe.
 */
@PlanningSolution
public class TimefoldPlan {

    private SlotOptions options;
    private List<TimefoldTask> tasks = List.of();
    private HardSoftLongScore score;

    /** Required by Timefold. */
    public TimefoldPlan() {
    }

    /**
     * @param options the frozen option table
     * @param tasks   the entities
     */
    public TimefoldPlan(SlotOptions options, List<TimefoldTask> tasks) {
        this.options = options;
        this.tasks = tasks;
    }

    @ProblemFactProperty
    public SlotOptions getOptions() {
        return options;
    }

    public void setOptions(SlotOptions options) {
        this.options = options;
    }

    @PlanningEntityCollectionProperty
    public List<TimefoldTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<TimefoldTask> tasks) {
        this.tasks = tasks;
    }

    @PlanningScore
    public HardSoftLongScore getScore() {
        return score;
    }

    public void setScore(HardSoftLongScore score) {
        this.score = score;
    }
}
