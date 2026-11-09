package eu.isygoit.solver.cpsat;

import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;
import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.params.LoadMetric;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.cpsat.contrib.ContribFixture;
import eu.isygoit.solver.cpsat.contrib.MaxConsecutiveContributor;
import eu.isygoit.solver.cpsat.contrib.MaxLoadPerPeriodContributor;
import eu.isygoit.solver.cpsat.contrib.MinRestContributor;
import eu.isygoit.solver.family.FamilyKit;
import eu.isygoit.solver.family.MaxConsecutiveFamily;
import eu.isygoit.solver.family.MaxLoadPerPeriodFamily;
import eu.isygoit.solver.family.MinRestFamily;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static eu.isygoit.solver.family.FamilyKit.map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Golden scenario G9 (design 2.6 c, 9.5) at the contributor level: five pinned 8-hour shifts over three nurses with a hard
 * 11-hour rest, a hard "8 ticks per day" load, a soft "at most 2 consecutive days" and a min-max fairness objective. Expected
 * optimum: max load 16, {@code d1-night} on {@code ana} (the only nurse with the icu skill), no soft violation, and the
 * independent checkers of the three families agree with the model (no violation on the solved plan).
 */
class RosterScenarioTest {

    private static final String[] SHIFTS = {"d0-day", "d0-night", "d1-day", "d1-night", "d2-day"};
    private static final int[] STARTS = {8, 22, 32, 46, 56};

    @Test
    void rosterOptimumIsSixteenWithTheIcuShiftOnAna() {
        Assumptions.assumeTrue(ContribFixture.nativeAvailable(), "OR-Tools natives not available");

        Constraint rest = FamilyKit.hard("rest-11h", "min_rest", map("minRestTicks", 11));
        Constraint daily = FamilyKit.hard("one-shift-per-day", "max_load_per_period",
                map("period", map("unit", "DAY"), "metric", "TICKS", "max", 8));
        Constraint consecutive = FamilyKit.constraint("max-2-days-in-a-row", "max_consecutive",
                IEnumEnforcement.Types.SOFT, null, 10, map("period", map("unit", "DAY"), "max", 2));

        List<eu.isygoit.dto.request.Task> tasks = new java.util.ArrayList<>();
        for (String shift : SHIFTS) {
            tasks.add(FamilyKit.task(shift, 8));
        }
        PlanningProblem problem = FamilyKit.problem(FamilyKit.horizon(72, 3600), "UTC",
                List.of(FamilyKit.resource("ana", 1), FamilyKit.resource("ben", 1), FamilyKit.resource("cleo", 1)), tasks,
                List.of(rest, daily, consecutive));

        FamilyKit.View view = FamilyKit.View.of(problem);
        for (int i = 0; i < SHIFTS.length; i++) {
            view.pinned(SHIFTS[i], STARTS[i]);
        }
        // skills and availability: cleo is off during the d0 day shift, d1-night needs icu (ana only)
        view.candidate("d0-day", "ana").candidate("d0-day", "ben");
        view.candidate("d0-night", "ana").candidate("d0-night", "ben").candidate("d0-night", "cleo");
        view.candidate("d1-day", "ana").candidate("d1-day", "ben").candidate("d1-day", "cleo");
        view.candidate("d1-night", "ana");
        view.candidate("d2-day", "ana").candidate("d2-day", "ben").candidate("d2-day", "cleo");

        ContribFixture f = ContribFixture.of(view);
        ContribFixture.FakeEnforcer restEnforcer = new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, f);
        ContribFixture.FakeEnforcer dailyEnforcer = new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, f);
        ContribFixture.FakeEnforcer softEnforcer = new ContribFixture.FakeEnforcer(Enforcer.Mode.SOFT, f);
        MinRestFamily restFamily = new MinRestFamily();
        MaxLoadPerPeriodFamily loadFamily = new MaxLoadPerPeriodFamily();
        MaxConsecutiveFamily consecutiveFamily = new MaxConsecutiveFamily();
        new MinRestContributor().apply(f.ctx, f.compile(restFamily, rest), restEnforcer);
        new MaxLoadPerPeriodContributor().apply(f.ctx, f.compile(loadFamily, daily), dailyEnforcer);
        new MaxConsecutiveContributor().apply(f.ctx, f.compile(consecutiveFamily, consecutive), softEnforcer);

        // minimize_max_load (tier 0) then the soft penalty: 1000 * L + penalty
        IntVar maxLoad = f.model.newIntVar(0, 40, "maxLoad");
        for (int r = 0; r < 3; r++) {
            f.model.addGreaterOrEqual(maxLoad, f.ctx.load(r, LoadMetric.TICKS));
        }
        LinearExprBuilder objective = LinearExpr.newBuilder().addTerm(maxLoad, 1000).add(softEnforcer.penalty());
        ContribFixture.Solved solved = f.solve(objective.build());

        assertTrue(solved.solved());
        assertEquals(16, solved.solver.value(maxLoad), "five 8-tick shifts over three nurses: best max load is two shifts");
        assertEquals(16_000, Math.round(solved.objective()), "no soft violation");
        assertTrue(solved.on("d1-night", "ana"));

        FamilyKit.Solution plan = solved.toSolution();
        assertEquals(0, FamilyKit.run(restFamily, problem, view, plan, rest).count(), "rest rule");
        assertEquals(0, FamilyKit.run(loadFamily, problem, view, plan, daily).count(), "daily load rule");
        assertEquals(0, FamilyKit.run(consecutiveFamily, problem, view, plan, consecutive).count(), "consecutive days");
    }

    @Test
    void restRuleChainNeedsTwoNursesAndOneIsNotEnough() {
        Assumptions.assumeTrue(ContribFixture.nativeAvailable(), "OR-Tools natives not available");
        Constraint rest = FamilyKit.hard("rest-11h", "min_rest", map("minRestTicks", 11));
        // consecutive shifts are 6, 2, 6 and 2 hours apart, every other pair is far enough: a path, 2-colourable
        assertTrue(solveWith(rest, "ana").infeasible());
        ContribFixture.Solved two = solveWith(rest, "ana", "ben");
        assertTrue(two.solved());
        for (int i = 0; i + 1 < SHIFTS.length; i++) {
            assertTrue(two.on(SHIFTS[i], "ana") != two.on(SHIFTS[i + 1], "ana"), "consecutive shifts need different nurses");
        }
    }

    private ContribFixture.Solved solveWith(Constraint rest, String... nurses) {
        List<eu.isygoit.dto.request.Task> tasks = new java.util.ArrayList<>();
        for (String shift : SHIFTS) {
            tasks.add(FamilyKit.task(shift, 8));
        }
        List<eu.isygoit.dto.request.Resource> resources = new java.util.ArrayList<>();
        for (String nurse : nurses) {
            resources.add(FamilyKit.resource(nurse, 1));
        }
        PlanningProblem problem = FamilyKit.problem(FamilyKit.horizon(72, 3600), "UTC", resources, tasks, List.of(rest));
        FamilyKit.View view = FamilyKit.View.of(problem).allCandidates();
        for (int i = 0; i < SHIFTS.length; i++) {
            view.pinned(SHIFTS[i], STARTS[i]);
        }
        ContribFixture f = ContribFixture.of(view);
        new MinRestContributor().apply(f.ctx, f.compile(new MinRestFamily(), rest),
                new ContribFixture.FakeEnforcer(Enforcer.Mode.HARD, f));
        return f.solve(null);
    }
}
