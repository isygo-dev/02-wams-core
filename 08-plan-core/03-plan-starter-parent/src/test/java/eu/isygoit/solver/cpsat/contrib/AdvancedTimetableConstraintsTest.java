package eu.isygoit.solver.cpsat.contrib;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.Resource;
import eu.isygoit.dto.request.Task;
import eu.isygoit.dto.request.params.PreferredWindowsParams;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.family.FamilyKit;
import eu.isygoit.solver.family.MaxAdjacentTasksFamily;
import eu.isygoit.solver.family.MaxTasksPerPeriodFamily;
import eu.isygoit.solver.family.MaxWorkedPeriodsFamily;
import eu.isygoit.solver.family.PreferredWindowsFamily;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static eu.isygoit.solver.family.FamilyKit.map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancedTimetableConstraintsTest {

    @BeforeEach
    void requiresNatives() {
        Assumptions.assumeTrue(ContribFixture.nativeAvailable(), "OR-Tools natives not available");
    }

    @Test
    void dailyTaskLimitIsEnforcedForTasksInTheSameTagScope() {
        Constraint constraint = FamilyKit.hard("daily", MaxTasksPerPeriodFamily.TYPE,
                map("max", 1, "period", map("unit", "DAY")));
        List<Task> tasks = List.of(
                FamilyKit.task("one", 1, "req"),
                FamilyKit.task("two", 1, "req"));
        PlanningProblem problem = FamilyKit.problem(FamilyKit.horizon(48, 3600), "UTC",
                List.of(FamilyKit.resource("teacher", 1)), tasks, List.of(constraint));
        FamilyKit.View view = FamilyKit.View.of(problem)
                .candidate("one", "teacher").candidate("two", "teacher")
                .pinned("one", 1).pinned("two", 2);
        ContribFixture fixture = ContribFixture.of(view);

        new MaxTasksPerPeriodContributor().apply(fixture.ctx,
                fixture.compile(new MaxTasksPerPeriodFamily(), constraint),
                new ContribFixture.FakeEnforcer(eu.isygoit.solver.cpsat.Enforcer.Mode.HARD, fixture));

        assertTrue(fixture.solve(null).infeasible());
    }

    @Test
    void teacherDayLimitCountsWorkedDaysRatherThanTaskCount() {
        Constraint constraint = FamilyKit.constraint("days", MaxWorkedPeriodsFamily.TYPE,
                IEnumEnforcement.Types.STRICTLY_REQUIRED, new ConstraintScope("resource", "teacher"), 0,
                map("max", 1, "period", map("unit", "DAY")));
        PlanningProblem problem = FamilyKit.problem(FamilyKit.horizon(48, 3600), "UTC",
                List.of(FamilyKit.resource("teacher", 1)),
                List.of(FamilyKit.task("monday", 1), FamilyKit.task("tuesday", 1)), List.of(constraint));
        FamilyKit.View view = FamilyKit.View.of(problem)
                .candidate("monday", "teacher").candidate("tuesday", "teacher")
                .pinned("monday", 2).pinned("tuesday", 26);
        ContribFixture fixture = ContribFixture.of(view);

        new MaxWorkedPeriodsContributor().apply(fixture.ctx,
                fixture.compile(new MaxWorkedPeriodsFamily(), constraint),
                new ContribFixture.FakeEnforcer(eu.isygoit.solver.cpsat.Enforcer.Mode.HARD, fixture));

        assertTrue(fixture.solve(null).infeasible());
    }

    @Test
    void consecutiveTeacherSessionLimitForbidsAnOverlongAdjacentRun() {
        Constraint constraint = FamilyKit.constraint("adjacent", MaxAdjacentTasksFamily.TYPE,
                IEnumEnforcement.Types.STRICTLY_REQUIRED, new ConstraintScope("resource", "teacher"), 0,
                map("max", 2));
        PlanningProblem problem = FamilyKit.problem(FamilyKit.horizon(24, 3600), "UTC",
                List.of(FamilyKit.resource("teacher", 1)),
                List.of(FamilyKit.task("one", 1), FamilyKit.task("two", 1), FamilyKit.task("three", 1)),
                List.of(constraint));
        FamilyKit.View view = FamilyKit.View.of(problem)
                .candidate("one", "teacher").candidate("two", "teacher").candidate("three", "teacher")
                .pinned("one", 4).pinned("two", 5).pinned("three", 6);
        ContribFixture fixture = ContribFixture.of(view);

        new MaxAdjacentTasksContributor().apply(fixture.ctx,
                fixture.compile(new MaxAdjacentTasksFamily(), constraint),
                new ContribFixture.FakeEnforcer(eu.isygoit.solver.cpsat.Enforcer.Mode.HARD, fixture));

        assertTrue(fixture.solve(null).infeasible());
    }

    @Test
    void preferredWindowsAreSoftAndCheckerUsesTheSameFitSemantics() {
        Instant start = FamilyKit.START.plusSeconds(10 * 60L);
        Instant end = FamilyKit.START.plusSeconds(20 * 60L);
        Constraint constraint = FamilyKit.constraint("preferred", PreferredWindowsFamily.TYPE,
                IEnumEnforcement.Types.SOFT, ConstraintScope.all(), 1,
                Map.of("windows", List.of(Map.of("start", start, "end", end))));
        PlanningProblem problem = FamilyKit.problem(FamilyKit.horizon(30, 60), "UTC",
                List.of(FamilyKit.resource("teacher", 1)), List.of(FamilyKit.task("lesson", 5)),
                List.of(constraint));
        FamilyKit.View view = FamilyKit.View.of(problem).candidate("lesson", "teacher").pinned("lesson", 0);
        ContribFixture fixture = ContribFixture.of(view);
        ContribFixture.FakeEnforcer enforcer = new ContribFixture.FakeEnforcer(
                eu.isygoit.solver.cpsat.Enforcer.Mode.SOFT, fixture);

        new PreferredWindowsContributor().apply(fixture.ctx,
                fixture.compile(new PreferredWindowsFamily(), constraint), enforcer);
        ContribFixture.Solved solved = fixture.solve(enforcer.penalty());

        assertTrue(solved.solved());
        assertEquals(1, Math.round(solved.objective()));
        assertEquals(1, FamilyKit.run(new PreferredWindowsFamily(), problem, view, solved.toSolution(),
                constraint).count());
    }
}
