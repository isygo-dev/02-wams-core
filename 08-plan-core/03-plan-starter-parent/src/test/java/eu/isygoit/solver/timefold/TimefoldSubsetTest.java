package eu.isygoit.solver.timefold;

import eu.isygoit.dto.response.Assignment;
import eu.isygoit.dto.response.SolveResult;
import eu.isygoit.dto.response.TierValue;
import eu.isygoit.dto.response.UnassignedTask;
import eu.isygoit.enums.IEnumSolverStatus;
import eu.isygoit.solver.CompiledProblem;
import eu.isygoit.solver.SolveListener;
import eu.isygoit.solver.SolveProgress;
import eu.isygoit.solver.SolverCancellationToken;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Timefold subset on the golden scenarios G1, G3 and G12 of design 9.5. Pure Java (Timefold has no natives), so these are
 * normal unit tests; the same-objective comparison with CP-SAT is done by the golden tests of WP8b, here the expected
 * statuses and values of the design are asserted: Timefold never reports OPTIMAL, the tier value is the minimised
 * internal value ({@code -softScore}: slot cost 1.0 at scale 100 is 100; a maximisation of 9 is -9).
 */
class TimefoldSubsetTest {

    private final TimefoldSolverEngine engine = TimefoldTestProblems.engine();

    private SolveResult solve(CompiledProblem problem) {
        return engine.solve(problem, TimefoldTestProblems.context(problem.problem().options(),
                new SolverCancellationToken(), SolveListener.NOOP));
    }

    /** G1: 2 sessions in the early slot (capacity 2), 1 in the late one; tier-0 value 100; FEASIBLE, never OPTIMAL. */
    @Test
    void g1CapacityFeasible() {
        SolveResult result = solve(TimefoldTestProblems.g1(true));

        assertThat(result.status()).isEqualTo(IEnumSolverStatus.Types.FEASIBLE);
        List<Assignment> assignments = result.solution().assignments();
        assertThat(assignments).hasSize(3);
        Map<String, Integer> perSlot = new TreeMap<>();
        for (Assignment a : assignments) {
            perSlot.merge(a.slotId(), 1, Integer::sum);
            assertThat(a.resources()).hasSize(1);
            assertThat(a.resources().get(0).resourceId()).isEqualTo("lab");
            assertThat(a.resources().get(0).amount()).isEqualTo(1);
            assertThat(a.endTick() - a.startTick()).isEqualTo(4);
            assertThat(a.optional()).isFalse();
        }
        assertThat(perSlot).containsEntry("slot-early", 2).containsEntry("slot-late", 1);
        assertThat(result.solution().unassignedTasks()).isEmpty();
        assertThat(result.metrics().tiers()).hasSize(1);
        TierValue tier = result.metrics().tiers().get(0);
        assertThat(tier.tier()).isZero();
        assertThat(tier.value()).isEqualTo(100L);
        assertThat(result.metrics().workersUsed()).isEqualTo(1);
    }

    /** G3: mandatory + optional-high assigned, medium and low NOT_SELECTED; tier-0 value -9 (reward 9, minimised form). */
    @Test
    void g3OptionalPriorities() {
        SolveResult result = solve(TimefoldTestProblems.g3(true));

        assertThat(result.status()).isEqualTo(IEnumSolverStatus.Types.FEASIBLE);
        List<String> assigned = new ArrayList<>();
        for (Assignment a : result.solution().assignments()) {
            assigned.add(a.taskId());
            assertThat(a.optional()).isEqualTo(!a.taskId().equals("mandatory-session"));
        }
        Collections.sort(assigned);
        assertThat(assigned).containsExactly("mandatory-session", "optional-high");
        List<String> unassigned = new ArrayList<>();
        for (UnassignedTask u : result.solution().unassignedTasks()) {
            unassigned.add(u.taskId());
            assertThat(u.reasonCode()).isEqualTo("NOT_SELECTED");
            assertThat(u.forgonePrize()).isEqualTo(1L);
        }
        assertThat(unassigned).containsExactly("optional-low", "optional-medium");
        assertThat(result.metrics().tiers().get(0).value()).isEqualTo(-9L);
    }

    /** G12: the school timetable: 2 lessons on Monday, 2 on Tuesday; tier-0 value 200 (score 2.0). */
    @Test
    void g12SchoolTimetable() {
        SolveResult result = solve(TimefoldTestProblems.g12(true));

        assertThat(result.status()).isEqualTo(IEnumSolverStatus.Types.FEASIBLE);
        int monday = 0;
        int tuesday = 0;
        Map<String, String> teacherBySlot = new TreeMap<>();
        for (Assignment a : result.solution().assignments()) {
            if (a.slotId().startsWith("mon")) {
                monday++;
            } else {
                tuesday++;
            }
            String key = a.resources().get(0).resourceId() + "@" + a.slotId();
            assertThat(teacherBySlot.put(key, a.taskId())).as("teacher double booked: " + key).isNull();
        }
        assertThat(monday).isEqualTo(2);
        assertThat(tuesday).isEqualTo(2);
        assertThat(result.metrics().tiers().get(0).value()).isEqualTo(200L);
    }

    @Test
    void assignmentsAreSortedByStartThenTaskId() {
        SolveResult result = solve(TimefoldTestProblems.g12(true));

        List<Assignment> assignments = result.solution().assignments();
        for (int i = 1; i < assignments.size(); i++) {
            Assignment previous = assignments.get(i - 1);
            Assignment current = assignments.get(i);
            assertThat(previous.startTick() < current.startTick()
                    || (previous.startTick() == current.startTick()
                    && previous.taskId().compareTo(current.taskId()) <= 0)).isTrue();
        }
        assertThat(assignments.get(0).start()).isEqualTo(TimefoldTestProblems.g12(true).problem().horizon().toInstant(
                assignments.get(0).startTick()));
    }

    @Test
    void neverReportsOptimalNorInfeasible() {
        for (CompiledProblem problem : List.of(TimefoldTestProblems.g1(false), TimefoldTestProblems.g3(false))) {
            SolveResult result = solve(problem);
            assertThat(result.status()).isNotIn(IEnumSolverStatus.Types.OPTIMAL, IEnumSolverStatus.Types.INFEASIBLE);
        }
    }

    @Test
    void progressListenerSeesBuildAndSearch() {
        List<SolveProgress> events = new CopyOnWriteArrayList<>();
        SolveListener listener = new SolveListener() {
            @Override
            public void onProgress(SolveProgress progress) {
                events.add(progress);
            }
        };
        CompiledProblem problem = TimefoldTestProblems.g1(true);

        SolveResult result = engine.solve(problem, TimefoldTestProblems.context(problem.problem().options(),
                new SolverCancellationToken(), listener));

        assertThat(result.status()).isEqualTo(IEnumSolverStatus.Types.FEASIBLE);
        assertThat(events).extracting(SolveProgress::phase).contains("BUILD", "SEARCH");
        assertThat(events.stream().filter(e -> "SEARCH".equals(e.phase())).findFirst().orElseThrow().solutionCount())
                .isGreaterThanOrEqualTo(1);
    }

    @Test
    void incumbentsAreDeliveredOnlyWhenWanted() {
        List<Integer> sizes = new CopyOnWriteArrayList<>();
        SolveListener listener = new SolveListener() {
            @Override
            public boolean wantsIncumbents() {
                return true;
            }

            @Override
            public void onIncumbent(eu.isygoit.solver.IncumbentSnapshot snapshot) {
                sizes.add(snapshot.assignments().size());
            }
        };
        CompiledProblem problem = TimefoldTestProblems.g1(true);

        engine.solve(problem, TimefoldTestProblems.context(problem.problem().options(), new SolverCancellationToken(),
                listener));

        assertThat(sizes).isNotEmpty();
        assertThat(sizes.get(sizes.size() - 1)).isEqualTo(3);
    }

    @Test
    void cancellationBeforeSolveGivesUnknownWithoutSolution() {
        SolverCancellationToken token = new SolverCancellationToken();
        token.cancel();
        CompiledProblem problem = TimefoldTestProblems.g1(true);

        SolveResult result = engine.solve(problem, TimefoldTestProblems.context(problem.problem().options(), token,
                SolveListener.NOOP));

        assertThat(result.status()).isEqualTo(IEnumSolverStatus.Types.UNKNOWN);
        assertThat(result.solution()).isNull();
        assertThat(result.diagnostics().message()).isNotBlank();
    }

    @Test
    void deterministicRunsGiveTheSameAssignments() {
        SolveResult first = solve(TimefoldTestProblems.g12(true));
        SolveResult second = solve(TimefoldTestProblems.g12(true));

        assertThat(second.solution().assignments()).isEqualTo(first.solution().assignments());
    }
}
