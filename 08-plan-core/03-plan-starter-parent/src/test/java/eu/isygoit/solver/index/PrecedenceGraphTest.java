package eu.isygoit.solver.index;

import eu.isygoit.dto.request.PrecedenceRef;
import eu.isygoit.dto.request.Task;
import org.junit.jupiter.api.Test;

import java.util.List;

import static eu.isygoit.solver.index.IndexFixtures.task;
import static org.assertj.core.api.Assertions.assertThat;

class PrecedenceGraphTest {

    @Test
    void topologicalOrderOfAChainFollowsTheEdges() {
        int[] order = PrecedenceGraph.topologicalOrder(3, new int[]{1, 0}, new int[]{2, 1});
        assertThat(order).containsExactly(0, 1, 2);
    }

    @Test
    void topologicalOrderIsDeterministicForIndependentTasks() {
        int[] order = PrecedenceGraph.topologicalOrder(4, new int[0], new int[0]);
        assertThat(order).containsExactly(0, 1, 2, 3);
    }

    @Test
    void topologicalOrderOfADiamondPutsPredecessorsFirst() {
        // 0 -> 1, 0 -> 2, 1 -> 3, 2 -> 3
        int[] order = PrecedenceGraph.topologicalOrder(4, new int[]{0, 0, 1, 2}, new int[]{1, 2, 3, 3});
        assertThat(order).containsExactly(0, 1, 2, 3);
    }

    @Test
    void topologicalOrderIsNullWhenCyclic() {
        assertThat(PrecedenceGraph.topologicalOrder(2, new int[]{0, 1}, new int[]{1, 0})).isNull();
    }

    @Test
    void findCycleReturnsTheCycleInPrecedenceOrder() {
        // C precedes A, A precedes B, B precedes C
        List<Task> tasks = List.of(
                task("A").after("C").build(),
                task("B").after("A").build(),
                task("C").after("B").build(),
                task("D").build());
        List<String> cycle = PrecedenceGraph.findCycle(tasks);
        assertThat(cycle).hasSize(3).containsExactlyInAnyOrder("A", "B", "C");
        for (int i = 0; i < cycle.size(); i++) {
            String previous = cycle.get(i);
            String next = cycle.get((i + 1) % cycle.size());
            Task nextTask = tasks.stream().filter(t -> t.id().equals(next)).findFirst().orElseThrow();
            assertThat(nextTask.predecessors()).extracting(PrecedenceRef::taskId).contains(previous);
        }
    }

    @Test
    void findCycleIgnoresAcyclicGraphsSelfReferencesAndUnknownIds() {
        List<Task> tasks = List.of(
                task("A").build(),
                task("B").after("A").build(),
                task("C").after("C").after("ghost").build());
        assertThat(PrecedenceGraph.findCycle(tasks)).isEmpty();
    }

    @Test
    void propagateWindowsComputesTheCriticalPathOfAChain() {
        // A(3) -> B(2) -> C(4), finish-start, no lag, horizon 20
        int[] est = {0, 0, 0};
        int[] lst = {17, 18, 16};
        int[] dur = {3, 2, 4};
        boolean[] required = {true, true, true};
        int[] succStart = {0, 0, 1, 2};
        int[] pred = {0, 1};
        int[] type = {PrecedenceGraph.FINISH_START, PrecedenceGraph.FINISH_START};
        int[] lag = {0, 0};
        int[] topo = {0, 1, 2};
        int empty = PrecedenceGraph.propagateWindows(est, lst, dur, dur, required, succStart, pred, type, lag, topo);
        assertThat(est).containsExactly(0, 3, 5);
        assertThat(lst).containsExactly(11, 14, 16);
        assertThat(empty).isZero();
    }

    @Test
    void propagateWindowsReportsAnEmptyWindowWhenThePathDoesNotFit() {
        // A(10) -> B(10) in a horizon of 15 ticks
        int[] est = {0, 0};
        int[] lst = {5, 5};
        int[] dur = {10, 10};
        int empty = PrecedenceGraph.propagateWindows(est, lst, dur, dur, new boolean[]{true, true},
                new int[]{0, 0, 1}, new int[]{0}, new int[]{PrecedenceGraph.FINISH_START}, new int[]{0},
                new int[]{0, 1});
        assertThat(est[1]).isEqualTo(10);
        assertThat(lst[0]).isEqualTo(-5);
        assertThat(empty).isEqualTo(2);
    }

    @Test
    void propagateWindowsDoesNotRestrictThroughOptionalTasks() {
        int[] est = {0, 0};
        int[] lst = {17, 18};
        int[] dur = {3, 2};
        // A is optional: the forward bound on B is skipped; B is required: the backward bound on A applies
        PrecedenceGraph.propagateWindows(est, lst, dur, dur, new boolean[]{false, true},
                new int[]{0, 0, 1}, new int[]{0}, new int[]{PrecedenceGraph.FINISH_START}, new int[]{0},
                new int[]{0, 1});
        assertThat(est).containsExactly(0, 0);
        assertThat(lst).containsExactly(15, 18);
    }

    @Test
    void propagateWindowsSupportsStartStartAndLags() {
        int[] est = {0, 0};
        int[] lst = {50, 50};
        int[] dur = {5, 5};
        PrecedenceGraph.propagateWindows(est, lst, dur, dur, new boolean[]{true, true},
                new int[]{0, 0, 1}, new int[]{0}, new int[]{PrecedenceGraph.START_START}, new int[]{7},
                new int[]{0, 1});
        assertThat(est[1]).isEqualTo(7);
        assertThat(lst[0]).isEqualTo(43);
    }

    @Test
    void propagateWindowsSupportsFinishFinishUsingTheSuccessorDuration() {
        int[] est = {0, 0};
        int[] lst = {50, 50};
        int[] minDur = {10, 4};
        int[] maxDur = {10, 4};
        PrecedenceGraph.propagateWindows(est, lst, minDur, maxDur, new boolean[]{true, true},
                new int[]{0, 0, 1}, new int[]{0}, new int[]{PrecedenceGraph.FINISH_FINISH}, new int[]{0},
                new int[]{0, 1});
        // s_B + 4 >= s_A + 10  =>  s_B >= 6
        assertThat(est[1]).isEqualTo(6);
    }
}
