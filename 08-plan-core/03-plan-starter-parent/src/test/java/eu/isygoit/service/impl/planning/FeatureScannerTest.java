package eu.isygoit.service.impl.planning;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.InitialAssignment;
import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.request.RelaxationPolicy;
import eu.isygoit.dto.request.SolverOptions;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.EngineCapabilities.Feature;
import eu.isygoit.solver.index.IndexFixtures.ProblemB;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static eu.isygoit.solver.EngineCapabilities.Feature.CALENDARS;
import static eu.isygoit.solver.EngineCapabilities.Feature.COST_PER_USE;
import static eu.isygoit.solver.EngineCapabilities.Feature.CUMULATIVE_DEMAND;
import static eu.isygoit.solver.EngineCapabilities.Feature.DETERMINISTIC;
import static eu.isygoit.solver.EngineCapabilities.Feature.FREE_START;
import static eu.isygoit.solver.EngineCapabilities.Feature.INITIAL_SOLUTION;
import static eu.isygoit.solver.EngineCapabilities.Feature.LEXICOGRAPHIC;
import static eu.isygoit.solver.EngineCapabilities.Feature.LOCKED;
import static eu.isygoit.solver.EngineCapabilities.Feature.MAX_LOAD_TICKS;
import static eu.isygoit.solver.EngineCapabilities.Feature.MULTI_RESOURCE;
import static eu.isygoit.solver.EngineCapabilities.Feature.OPTIONAL_TASKS;
import static eu.isygoit.solver.EngineCapabilities.Feature.PINS;
import static eu.isygoit.solver.EngineCapabilities.Feature.PRECEDENCES;
import static eu.isygoit.solver.EngineCapabilities.Feature.RELAXATION;
import static eu.isygoit.solver.EngineCapabilities.Feature.RESOURCE_DURATIONS;
import static eu.isygoit.solver.EngineCapabilities.Feature.SETUPS;
import static eu.isygoit.solver.EngineCapabilities.Feature.SLOT_MODE;
import static eu.isygoit.solver.EngineCapabilities.Feature.TIME_WINDOWS;
import static eu.isygoit.solver.index.IndexFixtures.hard;
import static eu.isygoit.solver.index.IndexFixtures.problem;
import static eu.isygoit.solver.index.IndexFixtures.resource;
import static eu.isygoit.solver.index.IndexFixtures.skills;
import static eu.isygoit.solver.index.IndexFixtures.task;
import static org.assertj.core.api.Assertions.assertThat;

class FeatureScannerTest {

    private static Map<Feature, String> scan(ProblemB b) {
        return FeatureScanner.scan(b.problem());
    }

    private static ProblemB base() {
        return problem(100).resource(resource("r0")).resource(resource("r1"));
    }

    @Test
    void aPlainFreeStartRequestRequiresOnlyFreeStart() {
        assertThat(scan(base().task(task("a").duration(5)))).containsOnlyKeys(FREE_START);
    }

    @Test
    void listingFieldsWithDefaultValuesDoesNotRequireTheirFeatures() {
        // empty predecessors / requirements, capacity 1, demand 1: nothing but the free start
        ProblemB b = problem(100).resource(resource("r0").capacity(1)).task(task("a").duration(5).demand(1));
        assertThat(scan(b)).containsOnlyKeys(FREE_START);
    }

    @Test
    void slotModeReplacesFreeStart() {
        ProblemB b = base().slot("s0", 0, 5, 0).task(task("a").duration(5).slots("s0"));
        assertThat(scan(b)).containsOnlyKeys(SLOT_MODE);
        assertThat(scan(b).get(SLOT_MODE)).isEqualTo("tasks[0].startSlots");
    }

    @Test
    void resourceDurationsOptionalTasksAndMultiResourceAreReportedWithTheirPath() {
        ProblemB b = base()
                .task(task("a").duration(5).resourceDurations(Map.of("r0", 3)).requires(skills()))
                .task(task("b").duration(5).optional(9))
                .task(task("c").duration(5).requires("m", skills(), 2, null));
        Map<Feature, String> required = scan(b);
        assertThat(required.get(RESOURCE_DURATIONS)).isEqualTo("tasks[0].resourceDurations");
        assertThat(required.get(OPTIONAL_TASKS)).isEqualTo("tasks[1].required");
        assertThat(required.get(MULTI_RESOURCE)).isEqualTo("tasks[2].requirements[0].count");
    }

    @Test
    void moreThanOneRequirementIsMultiResource() {
        ProblemB b = base().task(task("a").duration(5).requires("x", skills(), 1, null).requires("y", skills(), 1, null));
        assertThat(scan(b).get(MULTI_RESOURCE)).isEqualTo("tasks[0].requirements");
    }

    @Test
    void capacityAboveOneOrAmountAboveOneIsCumulativeDemand() {
        assertThat(scan(problem(100).resource(resource("r0").capacity(3)).task(task("a").duration(5))).get(CUMULATIVE_DEMAND))
                .isEqualTo("resources[0].capacity");
        assertThat(scan(base().task(task("a").duration(5).demand(2))).get(CUMULATIVE_DEMAND))
                .isEqualTo("tasks[0].requirements[0].amount");
    }

    @Test
    void precedencesComeFromEdgesOrFromThePrecedenceConstraint() {
        ProblemB edges = base().task(task("a").duration(5)).task(task("b").duration(5).after("a"));
        assertThat(scan(edges).get(PRECEDENCES)).isEqualTo("tasks[1].predecessors");
        ProblemB constraint = base().task(task("a").duration(5)).constraint(hard("p", "precedence"));
        assertThat(scan(constraint).get(PRECEDENCES)).isEqualTo("constraints[0]");
    }

    @Test
    void timeWindowsComeFromReleaseDeadlineOrTheTimeWindowConstraint() {
        ProblemB b = base();
        b.task(task("a").duration(5).release(b.at(3)));
        assertThat(scan(b).get(TIME_WINDOWS)).isEqualTo("tasks[0].release");
        ProblemB d = base();
        d.task(task("a").duration(5).deadline(d.at(50)));
        assertThat(scan(d).get(TIME_WINDOWS)).isEqualTo("tasks[0].deadline");
        assertThat(scan(base().task(task("a").duration(5)).constraint(hard("w", "time_window"))).get(TIME_WINDOWS))
                .isEqualTo("constraints[0]");
    }

    @Test
    void calendarsSetupsCostPerUseAndMaxLoadAreResourceFeatures() {
        ProblemB b = problem(100);
        b.resource(resource("r0").calendar("cal")).resource(resource("r1").unavailable(b.at(1), b.at(2)))
                .resource(resource("r2").setupMatrix("m")).resource(resource("r3").costPerUse(3.0))
                .resource(resource("r4").maxLoad(10));
        b.task(task("a").duration(5));
        Map<Feature, String> required = scan(b);
        assertThat(required.get(CALENDARS)).isEqualTo("resources[0].calendarId");
        assertThat(required.get(SETUPS)).isEqualTo("resources[2].setupMatrixId");
        assertThat(required.get(COST_PER_USE)).isEqualTo("resources[3].costPerUse");
        assertThat(required.get(MAX_LOAD_TICKS)).isEqualTo("resources[4].maxLoadTicks");
        assertThat(scan(problem(100).resource(resource("r1").unavailable(b.at(1), b.at(2))).task(task("a").duration(5)))
                .get(CALENDARS)).isEqualTo("resources[0].unavailable");
    }

    @Test
    void pinsComeFromTaskPinsOrTheFixedAssignmentConstraint() {
        ProblemB b = base();
        b.task(task("a").duration(5).pin(b.at(5)));
        assertThat(scan(b).get(PINS)).isEqualTo("tasks[0].pin");
        assertThat(scan(base().task(task("a").duration(5)).constraint(hard("f", "fixed_assignment"))).get(PINS))
                .isEqualTo("constraints[0]");
    }

    @Test
    void initialSolutionAndLockedAreDistinguished() {
        ProblemB hint = base().task(task("a").duration(5)).initial(new InitialAssignment("a", null, List.of("r0"), false));
        assertThat(scan(hint)).containsKey(INITIAL_SOLUTION).doesNotContainKey(LOCKED);
        ProblemB locked = base().task(task("a").duration(5)).initial(new InitialAssignment("a", null, List.of("r0"), true));
        assertThat(scan(locked)).containsKeys(INITIAL_SOLUTION, LOCKED);
        assertThat(scan(base().task(task("a").duration(5)))).doesNotContainKey(INITIAL_SOLUTION);
    }

    @Test
    void lexicographicNeedsMoreThanOneDistinctUserTier() {
        ProblemB twoTiers = base().task(task("a").duration(5))
                .objective(new Objective("o1", "minimize_makespan", 1, 0, null, null, Map.of()))
                .objective(new Objective("o2", "minimize_resource_cost", 1, 1, null, null, Map.of()));
        assertThat(scan(twoTiers)).containsKey(LEXICOGRAPHIC);

        ProblemB sameTier = base().task(task("a").duration(5))
                .objective(new Objective("o1", "minimize_makespan", 1, 1, null, null, Map.of()))
                .objective(new Objective("o2", "minimize_resource_cost", 2, 1, null, null, Map.of()));
        assertThat(scan(sameTier)).doesNotContainKey(LEXICOGRAPHIC);

        ProblemB reservedAndDisabled = base().task(task("a").duration(5))
                .objective(new Objective("o1", "minimize_makespan", 1, 0, null, null, Map.of()))
                .objective(new Objective("auto-selected-value", "maximize_selected_value", 1, -1, null, null, Map.of()))
                .objective(new Objective("o3", "minimize_idle", 0, 5, null, null, Map.of()));
        assertThat(scan(reservedAndDisabled)).doesNotContainKey(LEXICOGRAPHIC);

        Constraint soft = new Constraint("s", "no_overlap", IEnumEnforcement.Types.SOFT, null, Map.of(), null, 5, 2);
        ProblemB softTier = base().task(task("a").duration(5)).constraint(soft)
                .objective(new Objective("o1", "minimize_makespan", 1, 0, null, null, Map.of()));
        assertThat(scan(softTier)).containsKey(LEXICOGRAPHIC);
    }

    @Test
    void relaxationRequiresAnAuthorizedPolicy() {
        Constraint authorized = new Constraint("r", "no_overlap", IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION,
                null, Map.of(), new RelaxationPolicy(true, 1, 1.0), 0, null);
        Constraint notAuthorized = new Constraint("r", "no_overlap", IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION,
                null, Map.of(), new RelaxationPolicy(false, 1, 1.0), 0, null);
        assertThat(scan(base().task(task("a").duration(5)).constraint(authorized)).get(RELAXATION))
                .isEqualTo("constraints[0]");
        assertThat(scan(base().task(task("a").duration(5)).constraint(notAuthorized))).doesNotContainKey(RELAXATION);
    }

    @Test
    void deterministicModeIsARequiredFeature() {
        SolverOptions deterministic = new SolverOptions(null, null, null, null, null, null, true, null, null, null,
                null, null, null);
        assertThat(scan(base().task(task("a").duration(5)).options(deterministic)).get(DETERMINISTIC))
                .isEqualTo("solverOptions.deterministic");
        assertThat(scan(base().task(task("a").duration(5)))).doesNotContainKey(DETERMINISTIC);
    }
}
