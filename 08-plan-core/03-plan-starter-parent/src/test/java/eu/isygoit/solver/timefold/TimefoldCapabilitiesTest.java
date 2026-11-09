package eu.isygoit.solver.timefold;

import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.EngineCapabilities;
import eu.isygoit.solver.EngineCapabilities.Feature;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.registry.ConstraintFamilyRegistry;
import eu.isygoit.solver.registry.ObjectiveFamilyRegistry;
import eu.isygoit.solver.registry.TimefoldContributorRegistry;
import eu.isygoit.solver.spi.ConstraintFamily;
import eu.isygoit.solver.spi.ObjectiveFamily;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The capability matrix of the Timefold subset (design 4.5): the exact feature set, the supported constraint and
 * objective types and their enforcement support, and the derivation from the registries.
 */
class TimefoldCapabilitiesTest {

    private static final Set<IEnumEnforcement.Types> ALL_ENFORCEMENTS = EnumSet.allOf(IEnumEnforcement.Types.class);

    private static ConstraintFamily family(String type, Set<IEnumEnforcement.Types> supported) {
        ConstraintFamily family = Mockito.mock(ConstraintFamily.class);
        Mockito.when(family.type()).thenReturn(type);
        Mockito.when(family.supportedEnforcements()).thenReturn(supported);
        return family;
    }

    private static ObjectiveFamily objective(String type) {
        ObjectiveFamily family = Mockito.mock(ObjectiveFamily.class);
        Mockito.when(family.type()).thenReturn(type);
        return family;
    }

    @Test
    void featureSetIsExactlyTheDocumentedSubset() {
        assertThat(TimefoldCapabilities.FEATURES).containsExactlyInAnyOrder(
                Feature.SLOT_MODE, Feature.DURATIONS_VARIABLE, Feature.CUMULATIVE_DEMAND, Feature.OPTIONAL_TASKS,
                Feature.TIME_WINDOWS, Feature.CALENDARS, Feature.PINS, Feature.DETERMINISTIC, Feature.PROGRESS);

        Set<Feature> missing = EnumSet.allOf(Feature.class);
        missing.removeAll(TimefoldCapabilities.FEATURES);
        assertThat(missing).containsExactlyInAnyOrder(
                Feature.FREE_START, Feature.RESOURCE_DURATIONS, Feature.PRECEDENCES, Feature.SETUPS,
                Feature.MULTI_RESOURCE, Feature.INITIAL_SOLUTION, Feature.LOCKED, Feature.LEXICOGRAPHIC,
                Feature.RELAXATION, Feature.EXPLAIN_ASSUMPTIONS, Feature.COST_PER_USE, Feature.MAX_LOAD_TICKS);
    }

    @Test
    void contributorsDeclareTheDocumentedSupport() {
        TimefoldContributorRegistry registry = TimefoldTestProblems.contributorRegistry();

        assertThat(registry.constraintTypes()).containsExactly("fixed_assignment", "resource_availability",
                "resource_capacity", "task_eligibility", "time_window");
        assertThat(registry.objectiveTypes()).containsExactly("maximize_selected_value", "maximize_task_priority",
                "minimize_resource_cost", "minimize_slot_cost");
        assertThat(registry.constraint("task_eligibility").orElseThrow().support())
                .isEqualTo(new EnforcementSupport(true, true, false));
        for (String type : List.of("resource_capacity", "time_window", "resource_availability", "fixed_assignment")) {
            assertThat(registry.constraint(type).orElseThrow().support()).isEqualTo(EnforcementSupport.HARD_ONLY);
        }
        for (String type : registry.constraintTypes()) {
            assertThat(registry.constraint(type).orElseThrow().support().relaxable()).as(type).isFalse();
        }
    }

    @Test
    void capabilitiesIntersectFamilySupportAndContributorSupport() {
        List<ConstraintFamily> families = new ArrayList<>();
        families.add(family("task_eligibility", ALL_ENFORCEMENTS));
        families.add(family("resource_capacity", ALL_ENFORCEMENTS));
        families.add(family("time_window", Set.of(IEnumEnforcement.Types.STRICTLY_REQUIRED)));
        families.add(family("no_overlap", ALL_ENFORCEMENTS));
        List<ObjectiveFamily> objectives = List.of(objective("minimize_slot_cost"), objective("minimize_makespan"));

        EngineCapabilities capabilities = TimefoldCapabilities.of(TimefoldTestProblems.contributorRegistry(),
                new ConstraintFamilyRegistry(families), new ObjectiveFamilyRegistry(objectives));

        assertThat(capabilities.engineId()).isEqualTo("timefold");
        assertThat(capabilities.provesOptimality()).isFalse();
        assertThat(capabilities.features()).isEqualTo(TimefoldCapabilities.FEATURES);
        // semantic AND formulation: relaxable is never available, soft only for task_eligibility
        assertThat(capabilities.constraints().get("task_eligibility")).isEqualTo(new EnforcementSupport(true, true, false));
        assertThat(capabilities.constraints().get("resource_capacity")).isEqualTo(EnforcementSupport.HARD_ONLY);
        assertThat(capabilities.constraints().get("time_window")).isEqualTo(EnforcementSupport.HARD_ONLY);
        // contributor without family, and family without contributor, are both absent
        assertThat(capabilities.constraints()).doesNotContainKeys("fixed_assignment", "resource_availability", "no_overlap");
        assertThat(capabilities.objectives()).containsExactly("minimize_slot_cost");
        assertThat(capabilities.unsupportedObjectiveTypes(List.of("minimize_slot_cost", "minimize_makespan")))
                .containsExactly("minimize_makespan");
    }

    @Test
    void everythingOutsideTheMatrixIsUnsupported() {
        EngineCapabilities capabilities = TimefoldCapabilities.of(TimefoldTestProblems.contributorRegistry(), null, null);

        assertThat(capabilities.unsupportedConstraintTypes(List.of("no_overlap", "precedence", "max_consecutive",
                "min_rest", "max_load_per_period", "multi_resource_requirement", "same_resource", "different_resource",
                "forbidden_pair", "max_tasks_per_resource", "task_eligibility", "time_window")))
                .containsExactly("different_resource", "forbidden_pair", "max_consecutive", "max_load_per_period",
                        "max_tasks_per_resource", "min_rest", "multi_resource_requirement", "no_overlap", "precedence",
                        "same_resource");
        assertThat(capabilities.unsupportedObjectiveTypes(List.of("minimize_makespan", "minimize_total_tardiness",
                "minimize_max_tardiness", "minimize_max_load", "balance_load", "minimize_idle", "minimize_setup",
                "minimize_preference_penalty", "minimize_plan_changes", "maximize_task_priority")))
                .containsExactly("balance_load", "minimize_idle", "minimize_makespan", "minimize_max_load",
                        "minimize_max_tardiness", "minimize_plan_changes", "minimize_preference_penalty",
                        "minimize_setup", "minimize_total_tardiness");
        assertThat(capabilities.supports(Feature.PRECEDENCES)).isFalse();
        assertThat(capabilities.supports(Feature.FREE_START)).isFalse();
        assertThat(capabilities.supports(Feature.SLOT_MODE)).isTrue();
    }

    @Test
    void engineExposesTheSameCapabilities() {
        TimefoldSolverEngine engine = TimefoldTestProblems.engine();

        assertThat(engine.name()).isEqualTo("timefold");
        assertThat(engine.capabilities().features()).isEqualTo(TimefoldCapabilities.FEATURES);
        assertThat(engine.capabilities().provesOptimality()).isFalse();
        assertThat(engine.capabilities().objectives()).hasSize(4);
    }
}
