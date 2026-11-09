package eu.isygoit.solver.cpsat;

import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.response.Relaxation;
import eu.isygoit.dto.response.SolveResult;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.enums.IEnumSolverStatus;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.CompiledObjective;
import eu.isygoit.solver.CompiledProblem;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static eu.isygoit.solver.cpsat.CpSatTestKit.assignment;
import static eu.isygoit.solver.cpsat.CpSatTestKit.on;
import static eu.isygoit.solver.cpsat.CpSatTestKit.options;
import static eu.isygoit.solver.cpsat.CpSatTestKit.resource;
import static eu.isygoit.solver.cpsat.CpSatTestKit.task;
import static eu.isygoit.solver.cpsat.CpSatTestKit.tier;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Relaxation in ONE solve (design 6.9, golden scenario G5): A and B (4 ticks each) share one machine; both have a RELAXABLE
 * latest end 6 with priorities 5 (A) and 0 (B). Both cannot hold, the cheapest set to drop is {B} at cost
 * {@code 1000 * (1 + 0)} in tier -2; the makespan (tier 0) is then 8. Skipped without the OR-Tools natives.
 */
class RelaxationSingleSolveTest {

    private static final CompiledObjective MAKESPAN = CpSatTestKit.objective(0, CpSatTestKit.MakespanContributor.TYPE, 1, 0);

    private final CpSatSolverEngine engine =
            CpSatTestKit.newEngine(CpSatTestKit.constraints(), CpSatTestKit.objectives(), new CpSatProperties());

    @BeforeAll
    static void requireNatives() {
        Assumptions.assumeTrue(CpSatTestKit.nativesAvailable(), "OR-Tools natives not available");
    }

    private PlanningProblem problem() {
        return CpSatTestKit.problem(30, List.of(resource("M", 1)),
                List.of(task("A", 4, true, null, List.of(on("M")), List.of()),
                        task("B", 4, true, null, List.of(on("M")), List.of())),
                options(10, 1, true));
    }

    private SolveResult solve(List<CompiledConstraint> constraints) {
        PlanningProblem problem = problem();
        CompiledProblem compiled = CpSatTestKit.compile(problem, constraints, List.of(MAKESPAN));
        return engine.solve(compiled, CpSatTestKit.context(problem.options(), null));
    }

    @Test
    void theCheapestConstraintIsDroppedInASingleModel() {
        SolveResult result = solve(List.of(
                CpSatTestKit.latestEnd(0, "deadline-A", "A", 6, IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION, 5),
                CpSatTestKit.latestEnd(1, "deadline-B", "B", 6, IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION, 0)));

        assertEquals(IEnumSolverStatus.Types.OPTIMAL, result.status(), String.valueOf(result.diagnostics()));
        assertEquals(1000L, tier(result, -2).value(), "dropping deadline-B costs 1000 * (1 + priority 0)");
        assertEquals(8L, tier(result, 0).value(), "makespan of the two tasks on one machine");
        assertEquals(2, result.metrics().tiers().size());
        assertEquals(-2, result.metrics().tiers().get(0).tier(), "tiers are reported most important first");

        List<Relaxation> relaxations = result.relaxations();
        assertEquals(1, relaxations.size());
        assertEquals("deadline-B", relaxations.get(0).constraintId());
        assertEquals("RELAXABLE_WITH_AUTHORIZATION", relaxations.get(0).originalValue());
        assertTrue(relaxations.get(0).authorized());
        assertEquals(List.of("deadline-B"), result.diagnostics().details().get("relaxedConstraintIds"));

        assertTrue(assignment(result, "A").endTick() <= 6, "deadline-A stays enforced");
        assertTrue(assignment(result, "B").endTick() > 6, "deadline-B was actually relaxed");
    }

    @Test
    void nothingIsRelaxedWhenTheProblemIsFeasible() {
        SolveResult result = solve(List.of(
                CpSatTestKit.latestEnd(0, "loose-A", "A", 20, IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION, 5),
                CpSatTestKit.latestEnd(1, "loose-B", "B", 20, IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION, 0)));

        assertEquals(IEnumSolverStatus.Types.OPTIMAL, result.status());
        assertEquals(0L, tier(result, -2).value());
        assertTrue(result.relaxations().isEmpty());
    }

    @Test
    void anUnauthorizedRelaxableConstraintIsHardAndMakesTheProblemInfeasible() {
        PlanningProblem problem = problem();
        // relaxationLevels restricts relaxation to an id that does not exist: both constraints stay HARD
        var restricted = new eu.isygoit.dto.request.SolverOptions(null, 10, 1, 0L, false, List.of("other"), true, null,
                null, null, null, null, null);
        PlanningProblem hard = new PlanningProblem(problem.jobId(), problem.domainName(), problem.timezone(),
                problem.horizon(), problem.slots(), problem.calendars(), problem.setupMatrices(), problem.resources(),
                problem.tasks(), problem.constraints(), problem.objectives(), restricted, problem.initialSolution());
        CompiledProblem compiled = CpSatTestKit.compile(hard, List.of(
                CpSatTestKit.latestEnd(0, "deadline-A", "A", 6, IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION, 5),
                CpSatTestKit.latestEnd(1, "deadline-B", "B", 6, IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION, 0)),
                List.of(MAKESPAN));
        SolveResult result = engine.solve(compiled, CpSatTestKit.context(restricted, null));
        assertEquals(IEnumSolverStatus.Types.INFEASIBLE, result.status());
    }
}
