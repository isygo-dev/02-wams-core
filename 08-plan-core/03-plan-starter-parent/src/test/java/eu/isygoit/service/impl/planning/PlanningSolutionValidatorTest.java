package eu.isygoit.service.impl.planning;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.InitialAssignment;
import eu.isygoit.dto.request.InitialSolution;
import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.PlanningProblem;
import eu.isygoit.dto.request.RelaxationPolicy;
import eu.isygoit.dto.request.SolverOptions;
import eu.isygoit.dto.response.Assignment;
import eu.isygoit.dto.response.Diagnostics;
import eu.isygoit.dto.response.Metrics;
import eu.isygoit.dto.response.ObjectiveScore;
import eu.isygoit.dto.response.PlanningSolution;
import eu.isygoit.dto.response.Relaxation;
import eu.isygoit.dto.response.SearchStats;
import eu.isygoit.dto.response.SolveResult;
import eu.isygoit.dto.response.TierValue;
import eu.isygoit.dto.response.UnassignedTask;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.enums.IEnumSolverStatus;
import eu.isygoit.exception.InvalidSolverResultException;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.CompiledObjective;
import eu.isygoit.solver.check.StubIndexView;
import eu.isygoit.solver.check.TestProblems;
import eu.isygoit.solver.registry.ConstraintFamilyRegistry;
import eu.isygoit.solver.registry.ObjectiveFamilyRegistry;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.CheckContext;
import eu.isygoit.solver.spi.ConstraintChecker;
import eu.isygoit.solver.spi.ConstraintFamily;
import eu.isygoit.solver.spi.ModelCost;
import eu.isygoit.solver.spi.ObjectiveEvaluator;
import eu.isygoit.solver.spi.ObjectiveFamily;
import eu.isygoit.solver.spi.PreprocessContext;
import eu.isygoit.solver.spi.ScopeKind;
import eu.isygoit.solver.spi.ValidationContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static eu.isygoit.solver.check.TestProblems.assign;
import static eu.isygoit.solver.check.TestProblems.at;
import static eu.isygoit.solver.check.TestProblems.requirement;
import static eu.isygoit.solver.check.TestProblems.resource;
import static eu.isygoit.solver.check.TestProblems.task;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanningSolutionValidatorTest {

    // ------------------------------------------------------------------------------------------ fake families

    /** Reports the violations produced by a lambda. */
    private record FakeConstraintFamily(String type, ConstraintChecker checker) implements ConstraintFamily {
        @Override
        public Class<?> paramsType() {
            return Void.class;
        }

        @Override
        public Set<IEnumEnforcement.Types> supportedEnforcements() {
            return Set.of(IEnumEnforcement.Types.values());
        }

        @Override
        public String descriptionKey() {
            return "plan.constraint." + type + ".description";
        }

        @Override
        public List<String> paramsHint() {
            return List.of();
        }

        @Override
        public ScopeKind scopeKind() {
            return ScopeKind.NONE;
        }

        @Override
        public void validate(ValidationContext ctx, Constraint constraint, Object params) {
        }

        @Override
        public ModelCost estimate(CandidateIndexView index, Constraint constraint, Object params) {
            return ModelCost.ZERO;
        }

        @Override
        public void preprocess(PreprocessContext ctx, Constraint constraint, Object params) {
        }
    }

    private record FakeObjectiveFamily(String type, boolean reward, ObjectiveEvaluator evaluator)
            implements ObjectiveFamily {
        @Override
        public Class<?> paramsType() {
            return Void.class;
        }

        @Override
        public String descriptionKey() {
            return "plan.objective." + type + ".description";
        }

        @Override
        public List<String> paramsHint() {
            return List.of();
        }

        @Override
        public void validate(ValidationContext ctx, Objective objective, Object params) {
        }

        @Override
        public ModelCost estimate(CandidateIndexView index, Objective objective, Object params) {
            return ModelCost.ZERO;
        }
    }

    private static ConstraintFamily family(String type, ConstraintChecker checker) {
        return new FakeConstraintFamily(type, checker);
    }

    private static final ConstraintChecker NEVER = (ctx, c, p, sink) -> {
    };

    private static Constraint constraint(String id, String type, IEnumEnforcement.Types enforcement, int weight,
                                         Integer tier, RelaxationPolicy relaxation) {
        return new Constraint(id, type, enforcement, ConstraintScope.all(), Map.of(), relaxation, weight, tier);
    }

    private static PlanningSolutionValidator validator() {
        return new PlanningSolutionValidator(new ConstraintFamilyRegistry(List.of()),
                new ObjectiveFamilyRegistry(List.of()));
    }

    private static List<CompiledConstraint> compiled(PlanningProblem problem, ConstraintFamily family) {
        List<CompiledConstraint> out = new ArrayList<>();
        for (int i = 0; i < problem.constraints().size(); i++) {
            out.add(new CompiledConstraint(i, problem.constraints().get(i), family, null, true));
        }
        return out;
    }

    private static SolveResult withMetrics(SolveResult result, TierValue... tiers) {
        Metrics metrics = new Metrics(1, 1, 1, null, 1, null, new SearchStats(0, 0, 1, null, null, null, 1),
                List.of(tiers));
        return new SolveResult(result.status(), result.solution(), result.relaxations(), result.diagnostics(), metrics);
    }

    private static InvalidSolverResultException rejects(PlanningSolutionValidator v, PlanningProblem p,
                                                         StubIndexView index, List<CompiledConstraint> cs,
                                                         List<CompiledObjective> os, SolveResult result) {
        return assertThrows(InvalidSolverResultException.class, () -> v.validateAndEnrich(p, index, cs, os, result));
    }

    private static PlanningProblem twoTasks(List<Constraint> constraints) {
        return TestProblems.problem(100, List.of(resource("R", 1)),
                List.of(task("A", 10).build(), task("B", 5).build(), task("O", 5).optional(9L).build()),
                constraints, List.of());
    }

    // ------------------------------------------------------------------------------------------ enrichment

    @Test
    void enrichmentSortsFillsReasonsAndScoresAnOptimalPlanOne() {
        PlanningProblem p = twoTasks(List.of());
        StubIndexView index = StubIndexView.of(p);
        SolveResult raw = TestProblems.result(IEnumSolverStatus.Types.OPTIMAL,
                assign("B", 10, 15, "R"), assign("A", 0, 10, "R"));
        SolveResult out = validator().validateAndEnrich(p, index, List.of(), List.of(), raw);
        PlanningSolution s = out.solution();
        assertEquals(List.of("A", "B"), s.assignments().stream().map(Assignment::taskId).toList());
        assertEquals(1, s.unassignedTasks().size());
        UnassignedTask unassigned = s.unassignedTasks().get(0);
        assertEquals("O", unassigned.taskId());
        assertEquals("NOT_SELECTED", unassigned.reasonCode());
        assertEquals(9L, unassigned.forgonePrize());
        assertEquals(1.0, s.qualityScore());
        assertTrue(s.softViolations().isEmpty() && s.hardViolations().isEmpty());
    }

    @Test
    void qualityScoreFollowsTheGap() {
        PlanningProblem p = twoTasks(List.of());
        StubIndexView index = StubIndexView.of(p);
        SolveResult feasible = TestProblems.result(IEnumSolverStatus.Types.FEASIBLE,
                assign("A", 0, 10, "R"), assign("B", 10, 15, "R"));
        assertEquals(0.5, validator().validateAndEnrich(p, index, List.of(), List.of(), feasible)
                .solution().qualityScore(), "unknown gap");
        Metrics metrics = new Metrics(1, 1, 1, null, 1, null, new SearchStats(0, 0, 1, 10.0, 8.0, 0.25, 1), List.of());
        SolveResult withGap = new SolveResult(feasible.status(), feasible.solution(), List.of(), null, metrics);
        assertEquals(0.8, validator().validateAndEnrich(p, index, List.of(), List.of(), withGap)
                .solution().qualityScore(), 1e-9);
    }

    @Test
    void resultsWithoutSolutionPassThroughOrFail() {
        PlanningProblem p = twoTasks(List.of());
        StubIndexView index = StubIndexView.of(p);
        SolveResult infeasible = new SolveResult(IEnumSolverStatus.Types.INFEASIBLE, null, List.of(), null, null);
        assertSame(infeasible, validator().validateAndEnrich(p, index, List.of(), List.of(), infeasible));
        SolveResult broken = new SolveResult(IEnumSolverStatus.Types.FEASIBLE, null, List.of(), null, null);
        InvalidSolverResultException e = rejects(validator(), p, index, List.of(), List.of(), broken);
        assertTrue(e.getMessage().startsWith("SOLUTION_INVALID_MISSING_SOLUTION"));
    }

    // ------------------------------------------------------------------------------------------ structural

    @Test
    void structuralViolationsAreRejectedWithTheirCode() {
        PlanningProblem p = twoTasks(List.of());
        StubIndexView index = StubIndexView.of(p);
        SolveResult overlap = TestProblems.result(IEnumSolverStatus.Types.FEASIBLE,
                assign("A", 0, 10, "R"), assign("B", 5, 10, "R"));
        assertTrue(rejects(validator(), p, index, List.of(), List.of(), overlap).getMessage()
                .startsWith("SOLUTION_INVALID_OVERLAP"));
        SolveResult unknown = TestProblems.result(IEnumSolverStatus.Types.FEASIBLE,
                assign("A", 0, 10, "R"), assign("B", 10, 15, "R"), assign("ghost", 20, 25, "R"));
        assertTrue(rejects(validator(), p, index, List.of(), List.of(), unknown).getMessage()
                .startsWith("SOLUTION_INVALID_TASK_UNKNOWN"));
        SolveResult duplicate = TestProblems.result(IEnumSolverStatus.Types.FEASIBLE,
                assign("A", 0, 10, "R"), assign("A", 20, 30, "R"), assign("B", 10, 15, "R"));
        assertTrue(rejects(validator(), p, index, List.of(), List.of(), duplicate).getMessage()
                .startsWith("SOLUTION_INVALID_TASK_DUPLICATE"));
        SolveResult missing = TestProblems.result(IEnumSolverStatus.Types.FEASIBLE, assign("A", 0, 10, "R"));
        assertTrue(rejects(validator(), p, index, List.of(), List.of(), missing).getMessage()
                .startsWith("SOLUTION_INVALID_TASK_MISSING"));
    }

    @Test
    void invalidUnassignedEntriesAreRejected() {
        PlanningProblem p = twoTasks(List.of());
        StubIndexView index = StubIndexView.of(p);
        SolveResult base = TestProblems.result(IEnumSolverStatus.Types.FEASIBLE,
                assign("A", 0, 10, "R"), assign("B", 10, 15, "R"));
        PlanningSolution forged = new PlanningSolution(base.solution().assignments(),
                List.of(new UnassignedTask("A", "NOT_SELECTED", "x", "y", 1)), List.of(), List.of(), List.of(), 1.0);
        SolveResult result = new SolveResult(base.status(), forged, List.of(), null, null);
        assertTrue(rejects(validator(), p, index, List.of(), List.of(), result).getMessage()
                .startsWith("SOLUTION_INVALID_UNASSIGNED"));
    }

    // ------------------------------------------------------------------------------------------ constraint families

    @Test
    void hardViolationOfAFamilyInvalidatesTheResult() {
        PlanningProblem p = twoTasks(List.of(
                constraint("hard-1", "fake", IEnumEnforcement.Types.STRICTLY_REQUIRED, 0, null, null)));
        StubIndexView index = StubIndexView.of(p);
        ConstraintFamily fam = family("fake", (ctx, c, params, sink) ->
                sink.violation("B", "R", "FAKE", "B is not allowed here", 1));
        SolveResult raw = TestProblems.result(IEnumSolverStatus.Types.FEASIBLE,
                assign("A", 0, 10, "R"), assign("B", 10, 15, "R"));
        InvalidSolverResultException e = rejects(validator(), p, index, compiled(p, fam), List.of(), raw);
        assertTrue(e.getMessage().startsWith("SOLUTION_INVALID_CONSTRAINT: constraint 'hard-1'"), e.getMessage());
        assertTrue(e.getMessage().contains("task 'B'"));
    }

    @Test
    void softViolationsAreReportedWithWeightTimesMeasureAndMatchTheTierValue() {
        PlanningProblem p = twoTasks(List.of(
                constraint("soft-1", "fake", IEnumEnforcement.Types.SOFT, 3, 0, null)));
        StubIndexView index = StubIndexView.of(p);
        ConstraintFamily fam = family("fake", (ctx, c, params, sink) -> {
            sink.violation("B", null, "FAKE", "late", 2);
            sink.violation("A", null, "FAKE", "late", 1);
        });
        SolveResult raw = TestProblems.result(IEnumSolverStatus.Types.OPTIMAL,
                assign("A", 0, 10, "R"), assign("B", 10, 15, "R"));
        SolveResult ok = validator().validateAndEnrich(p, index, compiled(p, fam), List.of(),
                withMetrics(raw, new TierValue(0, 9, null, "OPTIMAL")));
        assertEquals(2, ok.solution().softViolations().size());
        assertEquals("A", ok.solution().softViolations().get(0).taskId(), "sorted by task id");
        assertEquals(3.0, ok.solution().softViolations().get(0).impact());
        assertEquals(6.0, ok.solution().softViolations().get(1).impact());

        InvalidSolverResultException e = rejects(validator(), p, index, compiled(p, fam), List.of(),
                withMetrics(raw, new TierValue(0, 8, null, "OPTIMAL")));
        assertTrue(e.getMessage().startsWith(PlanningSolutionValidator.TIER_VALUE_MISMATCH + ": tier 0"));
        assertTrue(e.getMessage().contains("recomputed 9") && e.getMessage().contains("reported 8"));
    }

    // ------------------------------------------------------------------------------------------ objectives and tiers

    private static CompiledObjective objective(int index, String type, boolean reward, long raw, int weight,
                                               int tier, int scale) {
        Objective o = new Objective(type + "-" + index, type, weight, tier, null, null, Map.of());
        return new CompiledObjective(index, o, new FakeObjectiveFamily(type, reward, (CheckContext c, Objective ob,
                                                                                         Object params) -> raw),
                null, tier, scale);
    }

    @Test
    void objectivesAreReEvaluatedPerTierAndSignNormalised() {
        PlanningProblem p = twoTasks(List.of());
        StubIndexView index = StubIndexView.of(p);
        List<CompiledObjective> objectives = List.of(
                objective(0, "maximize_selected_value", true, 190, 1, -1, 100),
                objective(1, "minimize_makespan", false, 15, 2, 0, 100),
                objective(2, "minimize_slot_cost", false, 200, 1, 0, 100));
        SolveResult raw = TestProblems.result(IEnumSolverStatus.Types.OPTIMAL,
                assign("A", 0, 10, "R"), assign("B", 10, 15, "R"));
        SolveResult out = validator().validateAndEnrich(p, index, List.of(), objectives, withMetrics(raw,
                new TierValue(-2, 12345, null, "OPTIMAL"),        // relaxation tier is never compared
                new TierValue(-1, -190, null, "OPTIMAL"),
                new TierValue(0, 2 * 15 + 200, null, "OPTIMAL")));
        List<ObjectiveScore> scores = out.solution().qualityDetails();
        assertEquals(3, scores.size());
        assertEquals(190L, scores.get(0).rawValue());
        assertEquals(190.0, scores.get(0).score());
        assertEquals(190.0, scores.get(0).contribution());
        assertEquals(15.0, scores.get(1).score());
        assertEquals(30.0, scores.get(1).contribution());
        assertEquals(200L, scores.get(2).rawValue());
        assertEquals(2.0, scores.get(2).score(), "decimal objective: raw / scale");
    }

    @Test
    void wrongTierValueIsATierValueMismatchAndNotSolvedTiersAreSkipped() {
        PlanningProblem p = twoTasks(List.of());
        StubIndexView index = StubIndexView.of(p);
        List<CompiledObjective> objectives = List.of(objective(0, "minimize_makespan", false, 15, 1, 0, 100));
        SolveResult raw = TestProblems.result(IEnumSolverStatus.Types.FEASIBLE,
                assign("A", 0, 10, "R"), assign("B", 10, 15, "R"));
        InvalidSolverResultException e = rejects(validator(), p, index, List.of(), objectives,
                withMetrics(raw, new TierValue(0, 14, null, "FEASIBLE")));
        assertTrue(e.getMessage().startsWith("TIER_VALUE_MISMATCH"));
        validator().validateAndEnrich(p, index, List.of(), objectives,
                withMetrics(raw, new TierValue(0, 14, null, "NOT_SOLVED")));
        // Timefold style: no tiers at all
        validator().validateAndEnrich(p, index, List.of(), objectives, raw);
    }

    // ------------------------------------------------------------------------------------------ relaxation

    private static final RelaxationPolicy POLICY = new RelaxationPolicy(true, 1, 1.0);

    private PlanningProblem relaxableProblem() {
        return twoTasks(List.of(
                constraint("relax-1", "fake", IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION, 0, null, POLICY),
                constraint("strict-1", "ok", IEnumEnforcement.Types.STRICTLY_REQUIRED, 0, null, null)));
    }

    private List<CompiledConstraint> relaxableCompiled(PlanningProblem p, boolean violate) {
        ConstraintFamily violating = family("fake", violate
                ? (ctx, c, params, sink) -> {
                    sink.violation("B", "R", "FAKE", "b broke it", 4);
                    sink.violation("A", "R", "FAKE", "a broke it", 2);
                }
                : NEVER);
        ConstraintFamily fine = family("ok", NEVER);
        return List.of(new CompiledConstraint(0, p.constraints().get(0), violating, null, true),
                new CompiledConstraint(1, p.constraints().get(1), fine, null, true));
    }

    private static Relaxation engineRelaxation(String id) {
        return new Relaxation(id, "RELAXABLE_WITH_AUTHORIZATION", "RELAXED", 0.0, true, 0, List.of(), 0.0);
    }

    @Test
    void droppedRelaxableConstraintReportsItsRealImpact() {
        PlanningProblem p = relaxableProblem();
        StubIndexView index = StubIndexView.of(p);
        SolveResult raw = TestProblems.result(IEnumSolverStatus.Types.FEASIBLE,
                assign("A", 0, 10, "R"), assign("B", 10, 15, "R"));
        SolveResult claimed = new SolveResult(raw.status(), raw.solution(), List.of(engineRelaxation("relax-1")),
                null, null);
        SolveResult out = validator().validateAndEnrich(p, index, relaxableCompiled(p, true), List.of(), claimed);
        assertEquals(1, out.relaxations().size());
        Relaxation r = out.relaxations().get(0);
        assertEquals(2, r.violationCount());
        assertEquals(6.0, r.measure());
        assertEquals(List.of("A", "B"), r.violatedTaskIds());
        assertEquals(2, out.solution().hardViolations().size());
        assertEquals("A", out.solution().hardViolations().get(0).taskId());
    }

    @Test
    void harmlessRelaxationIsDroppedFromTheResult() {
        PlanningProblem p = relaxableProblem();
        StubIndexView index = StubIndexView.of(p);
        SolveResult raw = TestProblems.result(IEnumSolverStatus.Types.FEASIBLE,
                assign("A", 0, 10, "R"), assign("B", 10, 15, "R"));
        SolveResult claimed = new SolveResult(raw.status(), raw.solution(), List.of(engineRelaxation("relax-1")),
                null, null);
        SolveResult out = validator().validateAndEnrich(p, index, relaxableCompiled(p, false), List.of(), claimed);
        assertTrue(out.relaxations().isEmpty());
        assertTrue(out.solution().hardViolations().isEmpty());
    }

    @Test
    void relaxableConstraintThatWasNotDroppedMustHold() {
        PlanningProblem p = relaxableProblem();
        StubIndexView index = StubIndexView.of(p);
        SolveResult raw = TestProblems.result(IEnumSolverStatus.Types.FEASIBLE,
                assign("A", 0, 10, "R"), assign("B", 10, 15, "R"));
        InvalidSolverResultException e = rejects(validator(), p, index, relaxableCompiled(p, true), List.of(), raw);
        assertTrue(e.getMessage().startsWith("SOLUTION_INVALID_CONSTRAINT"));
    }

    @Test
    void relaxingAnUnauthorizedOrUnknownConstraintIsRejected() {
        PlanningProblem p = relaxableProblem();
        StubIndexView index = StubIndexView.of(p);
        SolveResult raw = TestProblems.result(IEnumSolverStatus.Types.FEASIBLE,
                assign("A", 0, 10, "R"), assign("B", 10, 15, "R"));
        for (String id : List.of("strict-1", "nobody")) {
            SolveResult claimed = new SolveResult(raw.status(), raw.solution(), List.of(engineRelaxation(id)),
                    null, null);
            assertTrue(rejects(validator(), p, index, relaxableCompiled(p, false), List.of(), claimed).getMessage()
                    .startsWith("SOLUTION_INVALID_RELAXATION"), id);
        }
        // not authorised by the policy
        Constraint unauthorised = constraint("relax-2", "fake", IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION,
                0, null, new RelaxationPolicy(false, 1, 1.0));
        PlanningProblem q = twoTasks(List.of(unauthorised));
        SolveResult claimed = new SolveResult(raw.status(), raw.solution(), List.of(engineRelaxation("relax-2")),
                null, null);
        assertTrue(rejects(validator(), q, StubIndexView.of(q), List.of(), List.of(), claimed).getMessage()
                .startsWith("SOLUTION_INVALID_RELAXATION"));
        // dropped but outside the relaxationLevels allow-list
        PlanningProblem listed = new PlanningProblem(1L, "t", "UTC", p.horizon(), List.of(), List.of(), List.of(),
                p.resources(), p.tasks(), p.constraints(), List.of(),
                new SolverOptions(null, null, null, null, null, List.of("other"), null, null, null, null, null,
                        null, null), new InitialSolution(null));
        SolveResult claimedListed = new SolveResult(raw.status(), raw.solution(), List.of(engineRelaxation("relax-1")),
                null, null);
        assertTrue(rejects(validator(), listed, StubIndexView.of(listed), List.of(), List.of(), claimedListed)
                .getMessage().startsWith("SOLUTION_INVALID_RELAXATION"));
    }

    // ------------------------------------------------------------------------------------------ warnings and registry

    @Test
    void startHintOutsideTheDomainBecomesAWarning() {
        PlanningProblem p = new PlanningProblem(1L, "t", "UTC", TestProblems.horizon(100), List.of(), List.of(),
                List.of(), List.of(resource("R", 1)), List.of(task("A", 10).deadline(50).build()), List.of(), List.of(),
                SolverOptions.defaults(),
                new InitialSolution(List.of(new InitialAssignment("A", at(70), List.of("R"), false))));
        StubIndexView index = StubIndexView.of(p);
        SolveResult raw = TestProblems.result(IEnumSolverStatus.Types.OPTIMAL, assign("A", 0, 10, "R"));
        Diagnostics diagnostics = validator().validateAndEnrich(p, index, List.of(), List.of(), raw).diagnostics();
        assertNotNull(diagnostics);
        assertEquals("HINT_OUT_OF_DOMAIN", diagnostics.warnings().get(0).code());
        assertEquals("initialSolution.assignments[0]", diagnostics.warnings().get(0).path());
    }

    @Test
    void familyIsResolvedFromTheRegistryWhenTheCompiledConstraintHasNone() {
        PlanningProblem p = twoTasks(List.of(
                constraint("hard-1", "fake", IEnumEnforcement.Types.STRICTLY_REQUIRED, 0, null, null)));
        StubIndexView index = StubIndexView.of(p);
        ConstraintFamily fam = family("fake", (ctx, c, params, sink) -> sink.violation(null, null, "X", "nope", 1));
        PlanningSolutionValidator v = new PlanningSolutionValidator(new ConstraintFamilyRegistry(List.of(fam)),
                new ObjectiveFamilyRegistry(List.of()));
        List<CompiledConstraint> cs = List.of(new CompiledConstraint(0, p.constraints().get(0), null, null, true));
        SolveResult raw = TestProblems.result(IEnumSolverStatus.Types.FEASIBLE,
                assign("A", 0, 10, "R"), assign("B", 10, 15, "R"));
        assertTrue(rejects(v, p, index, cs, List.of(), raw).getMessage().startsWith("SOLUTION_INVALID_CONSTRAINT"));
    }
}
