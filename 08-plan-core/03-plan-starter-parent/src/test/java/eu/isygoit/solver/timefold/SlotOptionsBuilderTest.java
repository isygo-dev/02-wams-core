package eu.isygoit.solver.timefold;

import eu.isygoit.dto.request.Horizon;
import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.Slot;
import eu.isygoit.dto.request.Task;
import eu.isygoit.dto.request.TimeWindow;
import eu.isygoit.exception.InvalidRequestException;
import eu.isygoit.exception.ObjectiveRangeException;
import eu.isygoit.solver.CompiledProblem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static eu.isygoit.solver.timefold.TimefoldTestProblems.T0;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link SlotOptionsBuilder}, {@link SlotOptions} and {@link TimefoldBuildContext#freeze()}: the int-indexed option table
 * (no R x S map), neutral pruning, defensive subset checks. Run lengths and tick values follow the horizon of
 * {@link TimefoldTestProblems} (tick = 15 minutes from 08:00Z, a one-hour slot = 4 ticks).
 */
class SlotOptionsBuilderTest {

    private static final Horizon HORIZON = new Horizon(T0, T0.plusSeconds(6 * 3600), 900);

    private static Slot slot(String id, int startTick, int endTick) {
        return TimefoldTestProblems.slot(id, T0.plusSeconds(startTick * 900L), T0.plusSeconds(endTick * 900L), 0.0);
    }

    private static CompiledProblem compile(List<Slot> slots, List<Resource> resources, List<Task> tasks) {
        return TimefoldTestProblems.compile(TimefoldTestProblems.problem(HORIZON, slots, resources, tasks, List.of(),
                TimefoldTestProblems.options(true)));
    }

    @Test
    void oneOptionPerCandidateAndSlotInContiguousCsr() {
        CompiledProblem compiled = compile(
                List.of(slot("s1", 0, 4), slot("s2", 4, 8)),
                List.of(TimefoldTestProblems.resource("r1", 1, Set.of(), null, null),
                        TimefoldTestProblems.resource("r2", 1, Set.of(), null, null)),
                List.of(TimefoldTestProblems.task("a", 4, true, 0, null, List.of("s1", "s2")),
                        TimefoldTestProblems.task("b", 4, true, 0, null, List.of("s2"))));

        SlotOptions options = SlotOptionsBuilder.build(compiled.index(), null);

        assertThat(options.taskCount()).isEqualTo(2);
        assertThat(options.taskOptionCount(0)).isEqualTo(4);
        assertThat(options.taskOptionCount(1)).isEqualTo(2);
        assertThat(options.optionCount()).isEqualTo(6);
        assertThat(options.taskOptionStart(1)).isEqualTo(options.taskOptionEnd(0));
        for (int o = 0; o < options.optionCount(); o++) {
            int slot = options.slot(o);
            assertThat(options.end(o) - options.start(o)).isEqualTo(4);
            assertThat(options.start(o)).isEqualTo(slot == 0 ? 0 : 4);
            assertThat(options.demand(o)).isEqualTo(1);
            assertThat(options.capacity(o)).isEqualTo(1);
            assertThat(options.penalty(o)).isZero();
            assertThat(options.taskOf(o)).isEqualTo(o < 4 ? 0 : 1);
        }
        // equal (resource, slot) share one capacity bucket key, different ones do not
        long distinctKeys = java.util.stream.IntStream.range(0, options.optionCount())
                .mapToLong(options::capacityKey).distinct().count();
        assertThat(distinctKeys).isEqualTo(4L);
    }

    @Test
    void slotsShorterThanTheDurationAreNotOptions() {
        CompiledProblem compiled = compile(
                List.of(slot("long", 0, 4), slot("short", 4, 6)),
                List.of(TimefoldTestProblems.resource("r1", 1, Set.of(), null, null)),
                List.of(TimefoldTestProblems.task("a", 4, true, 0, null, List.of("long", "short"))));

        SlotOptions options = SlotOptionsBuilder.build(compiled.index(), null);

        assertThat(options.optionCount()).isEqualTo(1);
        assertThat(options.slot(0)).isEqualTo(compiled.index().slotIdx("long"));
    }

    @Test
    void unavailableWindowRemovesTheSlotsItTouches() {
        TimeWindow blocked = new TimeWindow(T0, T0.plusSeconds(3600));
        CompiledProblem compiled = compile(
                List.of(slot("s1", 0, 4), slot("s2", 4, 8)),
                List.of(TimefoldTestProblems.resource("r1", 1, Set.of(), null, List.of(blocked))),
                List.of(TimefoldTestProblems.task("a", 4, true, 0, null, List.of("s1", "s2"))));

        SlotOptions options = SlotOptionsBuilder.build(compiled.index(), null);

        assertThat(options.optionCount()).isEqualTo(1);
        assertThat(options.start(0)).isEqualTo(4);
    }

    @Test
    void overlappingUsedSlotsAreRejected() {
        CompiledProblem compiled = compile(
                List.of(slot("s1", 0, 4), slot("s2", 2, 6)),
                List.of(TimefoldTestProblems.resource("r1", 1, Set.of(), null, null)),
                List.of(TimefoldTestProblems.task("a", 4, true, 0, null, List.of("s1", "s2"))));

        assertThatThrownBy(() -> SlotOptionsBuilder.build(compiled.index(), null))
                .isInstanceOfSatisfying(InvalidRequestException.class,
                        e -> assertThat(e.code()).isEqualTo("OVERLAPPING_SLOTS"));
    }

    @Test
    void touchingSlotsAreAllowed() {
        CompiledProblem compiled = compile(
                List.of(slot("s1", 0, 4), slot("s2", 4, 8)),
                List.of(TimefoldTestProblems.resource("r1", 1, Set.of(), null, null)),
                List.of(TimefoldTestProblems.task("a", 4, true, 0, null, List.of("s1", "s2"))));

        assertThat(SlotOptionsBuilder.build(compiled.index(), null).optionCount()).isEqualTo(2);
    }

    @Test
    void freeStartIsRejected() {
        CompiledProblem compiled = compile(
                List.of(slot("s1", 0, 4)),
                List.of(TimefoldTestProblems.resource("r1", 1, Set.of(), null, null)),
                List.of(TimefoldTestProblems.task("a", 4, true, 0, null, List.of())));

        assertThatThrownBy(() -> SlotOptionsBuilder.build(compiled.index(), null))
                .isInstanceOfSatisfying(InvalidRequestException.class,
                        e -> assertThat(e.code()).isEqualTo("ENGINE_UNSUPPORTED_FEATURE"));
    }

    // ------------------------------------------------------------------------------------------------ run helpers

    @Test
    void halfOpenRunsRequireTheWholeIntervalInsideOneRun() {
        int[] runs = {0, 4, 6, 10};
        assertThat(SlotOptionsBuilder.fitsAvailableRun(runs, 0, 4, 0, 4)).isTrue();
        assertThat(SlotOptionsBuilder.fitsAvailableRun(runs, 0, 4, 2, 5)).isFalse();
        assertThat(SlotOptionsBuilder.fitsAvailableRun(runs, 0, 4, 4, 6)).isFalse();
        assertThat(SlotOptionsBuilder.fitsAvailableRun(runs, 0, 4, 6, 10)).isTrue();
        assertThat(SlotOptionsBuilder.fitsAvailableRun(runs, 0, 4, 11, 12)).isFalse();
        assertThat(SlotOptionsBuilder.fitsAvailableRun(runs, 2, 4, 0, 4)).isFalse();
    }

    @Test
    void inclusiveRunsContainTheStartOnly() {
        int[] runs = {0, 3, 8, 8};
        assertThat(SlotOptionsBuilder.startInInclusiveRuns(runs, 0, 4, 3)).isTrue();
        assertThat(SlotOptionsBuilder.startInInclusiveRuns(runs, 0, 4, 4)).isFalse();
        assertThat(SlotOptionsBuilder.startInInclusiveRuns(runs, 0, 4, 8)).isTrue();
        assertThat(SlotOptionsBuilder.startInInclusiveRuns(runs, 0, 4, 9)).isFalse();
    }

    // ------------------------------------------------------------------------------------------------ context

    @Test
    void freezeDropsForbiddenAndOverCapacityOptionsAndKeepsPenalties() {
        CompiledProblem compiled = compile(
                List.of(slot("s1", 0, 4), slot("s2", 4, 8), slot("s3", 8, 12)),
                List.of(TimefoldTestProblems.resource("r1", 2, Set.of(), null, null)),
                List.of(TimefoldTestProblems.task("a", 4, true, 0, null, List.of("s1", "s2", "s3")),
                        TimefoldTestProblems.task("b", 4, false, 0, null, List.of("s1"))));
        SlotOptions base = SlotOptionsBuilder.build(compiled.index(), null);
        TimefoldBuildContext ctx = new TimefoldBuildContext(compiled, base);

        int s1 = base.taskOptionStart(0);
        ctx.forbid(s1 + 1, "c-forbid");
        ctx.forbid(s1 + 1, "other");
        ctx.addPenalty(s1, 7);
        ctx.addPenalty(s1, 5);
        ctx.addPenalty(s1 + 2, 3);
        ctx.addSelectionReward(1, 9);
        // capacity 2 lowered to 0 on the last option of task a: demand 1 > 0, dropped at freeze
        ctx.lowerCapacity(s1 + 2, 0);
        SlotOptions frozen = ctx.freeze();

        assertThat(ctx.isForbidden(s1 + 1)).isTrue();
        assertThat(ctx.forbiddenCount()).isEqualTo(1);
        assertThat(ctx.forbiddingConstraints(0)).containsExactly("c-forbid");
        assertThat(frozen.taskOptionCount(0)).isEqualTo(1);
        assertThat(frozen.taskOptionCount(1)).isEqualTo(1);
        assertThat(frozen.penalty(frozen.taskOptionStart(0))).isEqualTo(12L);
        assertThat(frozen.reward(0)).isZero();
        assertThat(frozen.reward(1)).isEqualTo(9L);
        assertThat(frozen.start(frozen.taskOptionStart(0))).isZero();
        assertThat(frozen.penaltiesCopy()).hasSize(2);
        assertThat(base.optionCount()).as("the base table is untouched").isEqualTo(4);
    }

    @Test
    void penaltiesAreGuardedToTheExactRange() {
        CompiledProblem compiled = compile(
                List.of(slot("s1", 0, 4)),
                List.of(TimefoldTestProblems.resource("r1", 1, Set.of(), null, null)),
                List.of(TimefoldTestProblems.task("a", 4, true, 0, null, List.of("s1"))));
        TimefoldBuildContext ctx = new TimefoldBuildContext(compiled, SlotOptionsBuilder.build(compiled.index(), null));

        ctx.addPenalty(0, 1L << 52);
        ctx.addPenalty(0, 1L << 52);
        assertThatThrownBy(() -> ctx.addPenalty(0, 1L)).isInstanceOf(ObjectiveRangeException.class);
    }

    @Test
    void scopeMasksAreNullForAllAndExactOtherwise() {
        Objective objective = new Objective("o", "minimize_slot_cost", 1);
        CompiledProblem compiled = compile(
                List.of(slot("s1", 0, 4)),
                List.of(TimefoldTestProblems.resource("r1", 1, Set.of(), null, null)),
                List.of(TimefoldTestProblems.task("a", 4, true, 0, null, List.of("s1"))));
        TimefoldBuildContext ctx = new TimefoldBuildContext(compiled, SlotOptionsBuilder.build(compiled.index(), null));

        assertThat(ctx.taskMask(objective.scope())).isNull();
        assertThat(ctx.resourceMask(null)).isNull();
        assertThat(TimefoldBuildContext.isResourceScope(new eu.isygoit.dto.request.ConstraintScope("resourceType", "x")))
                .isTrue();
        assertThat(TimefoldBuildContext.isResourceScope(new eu.isygoit.dto.request.ConstraintScope("tag", "x")))
                .isFalse();
    }
}
