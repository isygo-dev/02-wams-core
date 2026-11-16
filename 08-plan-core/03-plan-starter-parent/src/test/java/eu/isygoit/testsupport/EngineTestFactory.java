package eu.isygoit.testsupport;

import eu.isygoit.config.PlanLimitsProperties;
import eu.isygoit.service.impl.planning.*;
import eu.isygoit.solver.SolverEngine;
import eu.isygoit.solver.SolverRegistry;
import eu.isygoit.solver.cpsat.*;
import eu.isygoit.solver.cpsat.contrib.*;
import eu.isygoit.solver.cpsat.explain.CpSatExplainer;
import eu.isygoit.solver.cpsat.objective.*;
import eu.isygoit.solver.diag.PreSolveAnalyzer;
import eu.isygoit.solver.family.*;
import eu.isygoit.solver.objective.*;
import eu.isygoit.solver.registry.ConstraintFamilyRegistry;
import eu.isygoit.solver.registry.CpSatContributorRegistry;
import eu.isygoit.solver.registry.ObjectiveFamilyRegistry;
import eu.isygoit.solver.registry.TimefoldContributorRegistry;
import eu.isygoit.solver.spi.ConstraintFamily;
import eu.isygoit.solver.spi.ObjectiveFamily;
import eu.isygoit.solver.timefold.TimefoldConstraintContributor;
import eu.isygoit.solver.timefold.TimefoldObjectiveContributor;
import eu.isygoit.solver.timefold.TimefoldSolverEngine;
import eu.isygoit.solver.timefold.contrib.*;

import java.util.List;

/**
 * Hand-wired Engine v2 object graph for tests that run without Spring (golden, consistency, guard and rejection tests).
 *
 * <p>Contract: every method builds a FRESH graph (no static state), so tests are isolated and may run in parallel. The
 * lists returned by {@link #constraintFamilies()}, {@link #objectiveFamilies()}, {@link #cpSatConstraintContributors()},
 * {@link #cpSatObjectiveContributors()}, {@link #timefoldConstraintContributors()} and
 * {@link #timefoldObjectiveContributors()} are the COMPLETE catalogue of the design (sections 4.3, 4.4 and 4.5): they are the
 * single place to add a new family/contributor bean for tests. Families, contributors and objectives are plain components
 * with no-arg constructors; only the engines and the service use collaborators.
 *
 * <p>Thread-safety: this class is stateless; the returned objects have the thread-safety of their own classes (the engines,
 * registries and the service are safe to share, which is how Spring uses them).
 *
 * <p>Units: not applicable (wiring only).
 *
 * <p>Constructor shapes of the classes owned by the other Wave 2 work packages (engines, builder, validators, analyzer,
 * service) are taken from the design text; every such spot carries a {@code WP8b-VERIFY: constructor} comment and is expected
 * to be adjusted by WP8b against the landed code.
 */
public final class EngineTestFactory {

    private EngineTestFactory() {
    }

    // ------------------------------------------------------------------ families (engine neutral)

    /**
     * All constraint families of the catalogue (design 4.3), in catalogue order.
     */
    public static List<ConstraintFamily> constraintFamilies() {
        return List.of(
                new TaskEligibilityFamily(),
                new ResourceCapacityFamily(),
                new NoOverlapFamily(),
                new PrecedenceFamily(),
                new TimeWindowFamily(),
                new ResourceAvailabilityFamily(),
                new MaxConsecutiveFamily(),
                new MinRestFamily(),
                new MaxLoadPerPeriodFamily(),
                new MultiResourceRequirementFamily(),
                new SameResourceFamily(),
                new DifferentResourceFamily(),
                new FixedAssignmentFamily(),
                new ForbiddenPairFamily(),
                new MaxTasksPerResourceFamily());
    }

    /**
     * All objective families of the catalogue (design 4.4), in catalogue order.
     */
    public static List<ObjectiveFamily> objectiveFamilies() {
        return List.of(
                new MinimizeMakespanObjective(),
                new MinimizeTotalTardinessObjective(),
                new MinimizeMaxTardinessObjective(),
                new MinimizeResourceCostObjective(),
                new MinimizeSlotCostObjective(),
                new MaximizeTaskPriorityObjective(),
                new MaximizeSelectedValueObjective(),
                new MinimizeMaxLoadObjective(),
                new BalanceLoadObjective(),
                new MinimizeIdleObjective(),
                new MinimizeSetupObjective(),
                new MinimizePreferencePenaltyObjective(),
                new MinimizePlanChangesObjective());
    }

    // ------------------------------------------------------------------ CP-SAT contributors

    /**
     * CP-SAT constraint contributors, one per family (design 10.1 WP4a/WP4b).
     */
    public static List<CpSatConstraintContributor> cpSatConstraintContributors() {
        return List.of(
                new TaskEligibilityContributor(),
                new ResourceCapacityContributor(),
                new NoOverlapContributor(),
                new PrecedenceContributor(),
                new TimeWindowContributor(),
                new ResourceAvailabilityContributor(),
                new MaxConsecutiveContributor(),
                new MinRestContributor(),
                new MaxLoadPerPeriodContributor(),
                new MultiResourceRequirementContributor(),
                new SameResourceContributor(),
                new DifferentResourceContributor(),
                new FixedAssignmentContributor(),
                new ForbiddenPairContributor(),
                new MaxTasksPerResourceContributor());
    }

    /**
     * CP-SAT objective contributors, one per objective family (design 10.1 WP4c).
     */
    public static List<CpSatObjectiveContributor> cpSatObjectiveContributors() {
        // WP8b-VERIFY: class names (design 10.1 says "<Name>Contributor" for each objective, e.g. MinimizeMakespanContributor)
        return List.of(
                new MinimizeMakespanContributor(),
                new MinimizeTotalTardinessContributor(),
                new MinimizeMaxTardinessContributor(),
                new MinimizeResourceCostContributor(),
                new MinimizeSlotCostContributor(),
                new MaximizeTaskPriorityContributor(),
                new MaximizeSelectedValueContributor(),
                new MinimizeMaxLoadContributor(),
                new BalanceLoadContributor(),
                new MinimizeIdleContributor(),
                new MinimizeSetupContributor(),
                new MinimizePreferencePenaltyContributor(),
                new MinimizePlanChangesContributor());
    }

    // ------------------------------------------------------------------ Timefold contributors (subset, design 4.5)

    /**
     * Timefold constraint contributors: the frozen subset of design 4.3/4.5 (neutral pruning families).
     */
    public static List<TimefoldConstraintContributor> timefoldConstraintContributors() {
        return List.of(
                new TaskEligibilityTimefoldContributor(),
                new ResourceCapacityTimefoldContributor(),
                new TimeWindowTimefoldContributor(),
                new ResourceAvailabilityTimefoldContributor(),
                new FixedAssignmentTimefoldContributor());
    }

    /**
     * Timefold objective contributors: slot cost, resource cost, task priority, selected value.
     */
    public static List<TimefoldObjectiveContributor> timefoldObjectiveContributors() {
        return List.of(
                new MinimizeSlotCostTimefoldContributor(),
                new MinimizeResourceCostTimefoldContributor(),
                new MaximizeTaskPriorityTimefoldContributor(),
                new MaximizeSelectedValueTimefoldContributor());
    }

    // ------------------------------------------------------------------ registries

    public static ConstraintFamilyRegistry constraintFamilyRegistry() {
        return new ConstraintFamilyRegistry(constraintFamilies());
    }

    public static ObjectiveFamilyRegistry objectiveFamilyRegistry() {
        return new ObjectiveFamilyRegistry(objectiveFamilies());
    }

    public static CpSatContributorRegistry cpSatContributorRegistry() {
        return new CpSatContributorRegistry(cpSatConstraintContributors(), cpSatObjectiveContributors());
    }

    public static TimefoldContributorRegistry timefoldContributorRegistry() {
        return new TimefoldContributorRegistry(timefoldConstraintContributors(), timefoldObjectiveContributors());
    }

    // ------------------------------------------------------------------ engines

    /**
     * CP-SAT structural plug-ins in apply order (setup circuit 700, symmetry breaking 800).
     */
    public static List<CpSatStructuralModule> cpSatStructuralModules(CpSatProperties properties) {
        // WP8b-VERIFY: constructor (SetupCircuitModule, SymmetryBreakingModule)
        return List.of(new SetupCircuitModule(), new SymmetryBreakingModule(properties));
    }

    /**
     * CP-SAT engine with default properties (needs the OR-Tools natives at solve time, not at construction).
     */
    public static CpSatSolverEngine cpSatEngine() {
        return cpSatEngine(new CpSatProperties());
    }

    public static CpSatSolverEngine cpSatEngine(CpSatProperties properties) {
        CpModelBuilder builder = cpSatModelBuilder(properties);
        // WP8b-VERIFY: constructor (design 5.3 / 10.1 WP3: engine = builder + explainer + properties)
        return new CpSatSolverEngine(builder, new CpSatExplainer(), properties);
    }

    /**
     * The CP-SAT model builder over the full contributor registry and the structural modules.
     */
    public static CpModelBuilder cpSatModelBuilder(CpSatProperties properties) {
        // WP8b-VERIFY: constructor (registry, structural modules, properties)
        return new CpModelBuilder(cpSatContributorRegistry(), cpSatStructuralModules(properties), properties);
    }

    /**
     * Timefold engine over the full Timefold contributor registry.
     */
    public static TimefoldSolverEngine timefoldEngine() {
        // WP8b-VERIFY: constructor
        return new TimefoldSolverEngine(timefoldContributorRegistry(), constraintFamilyRegistry(), objectiveFamilyRegistry());
    }

    public static SolverRegistry solverRegistry(SolverEngine... engines) {
        return new SolverRegistry(List.of(engines));
    }

    // ------------------------------------------------------------------ service

    /**
     * Service with both engines, default limits.
     */
    public static PlanningService planningService() {
        return planningService(new PlanLimitsProperties());
    }

    /**
     * Service with both engines and the given limits (guard tests lower the limits).
     */
    public static PlanningService planningService(PlanLimitsProperties limits) {
        return planningService(limits, solverRegistry(cpSatEngine(), timefoldEngine()));
    }

    /**
     * Service over an explicit engine registry (tests that plug a fake engine).
     */
    public static PlanningService planningService(PlanLimitsProperties limits, SolverRegistry solvers) {
        ConstraintFamilyRegistry constraintFamilies = constraintFamilyRegistry();
        ObjectiveFamilyRegistry objectiveFamilies = objectiveFamilyRegistry();
        // WP8b-VERIFY: constructor (every argument list below follows design 0.3 pipeline order)
        PlanningRequestValidator validator = new PlanningRequestValidator(limits);
        DomainNormalizer normalizer = new DomainNormalizer(constraintFamilies);
        ProblemVerifier verifier = new ProblemVerifier(constraintFamilies, objectiveFamilies, limits);
        PreSolveAnalyzer analyzer = new PreSolveAnalyzer();
        PlanningSolutionValidator solutionValidator =
                new PlanningSolutionValidator(constraintFamilies, objectiveFamilies);
        return new PlanningService(validator, normalizer, verifier, new ModelCostEstimator(limits),
                constraintFamilies, objectiveFamilies, solvers, analyzer, solutionValidator,
                new SolveConcurrencyGuard(limits), limits);
    }
}
