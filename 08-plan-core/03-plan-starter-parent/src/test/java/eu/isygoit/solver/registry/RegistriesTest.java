package eu.isygoit.solver.registry;

import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.Objective;
import eu.isygoit.dto.response.SolveResult;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.exception.UnknownConstraintException;
import eu.isygoit.exception.UnknownObjectiveException;
import eu.isygoit.exception.UnknownSolverException;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.CompiledObjective;
import eu.isygoit.solver.CompiledProblem;
import eu.isygoit.solver.EngineCapabilities;
import eu.isygoit.solver.SolveContext;
import eu.isygoit.solver.SolverEngine;
import eu.isygoit.solver.SolverRegistry;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatConstraintContributor;
import eu.isygoit.solver.cpsat.CpSatObjectiveContributor;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.cpsat.ObjectiveSink;
import eu.isygoit.solver.spi.CandidateIndexView;
import eu.isygoit.solver.spi.ConstraintChecker;
import eu.isygoit.solver.spi.ConstraintFamily;
import eu.isygoit.solver.spi.ModelCost;
import eu.isygoit.solver.spi.ObjectiveEvaluator;
import eu.isygoit.solver.spi.ObjectiveFamily;
import eu.isygoit.solver.spi.ScopeKind;
import eu.isygoit.solver.spi.ValidationContext;
import eu.isygoit.solver.timefold.TimefoldBuildContext;
import eu.isygoit.solver.timefold.TimefoldConstraintContributor;
import eu.isygoit.solver.timefold.TimefoldObjectiveContributor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistriesTest {

    // ------------------------------------------------------------------ stubs

    static class StubFamily implements ConstraintFamily {
        private final String type;

        StubFamily(String type) {
            this.type = type;
        }

        public String type() {
            return type;
        }

        public Class<?> paramsType() {
            return Void.class;
        }

        public Set<IEnumEnforcement.Types> supportedEnforcements() {
            return Set.of(IEnumEnforcement.Types.STRICTLY_REQUIRED);
        }

        public String descriptionKey() {
            return "plan.constraint." + type + ".description";
        }

        public List<String> paramsHint() {
            return List.of();
        }

        public ScopeKind scopeKind() {
            return ScopeKind.NONE;
        }

        public void validate(ValidationContext ctx, Constraint constraint, Object params) {
        }

        public ModelCost estimate(CandidateIndexView index, Constraint constraint, Object params) {
            return ModelCost.ZERO;
        }

        public ConstraintChecker checker() {
            return (ctx, constraint, params, sink) -> {
            };
        }
    }

    static class OtherStubFamily extends StubFamily {
        OtherStubFamily(String type) {
            super(type);
        }
    }

    static class StubObjective implements ObjectiveFamily {
        private final String type;

        StubObjective(String type) {
            this.type = type;
        }

        public String type() {
            return type;
        }

        public Class<?> paramsType() {
            return Void.class;
        }

        public String descriptionKey() {
            return "plan.objective." + type + ".description";
        }

        public List<String> paramsHint() {
            return List.of();
        }

        public boolean reward() {
            return false;
        }

        public void validate(ValidationContext ctx, Objective objective, Object params) {
        }

        public ModelCost estimate(CandidateIndexView index, Objective objective, Object params) {
            return ModelCost.ZERO;
        }

        public ObjectiveEvaluator evaluator() {
            return (ctx, objective, params) -> 0L;
        }
    }

    static class CpConstraint implements CpSatConstraintContributor {
        private final String type;

        CpConstraint(String type) {
            this.type = type;
        }

        public String type() {
            return type;
        }

        public EnforcementSupport support() {
            return EnforcementSupport.HARD_ONLY;
        }

        public void apply(CpModelContext ctx, CompiledConstraint constraint, Enforcer enforcer) {
        }
    }

    static class CpObjective implements CpSatObjectiveContributor {
        private final String type;

        CpObjective(String type) {
            this.type = type;
        }

        public String type() {
            return type;
        }

        public void contribute(CpModelContext ctx, CompiledObjective objective, ObjectiveSink sink) {
        }
    }

    static class TfConstraint implements TimefoldConstraintContributor {
        private final String type;

        TfConstraint(String type) {
            this.type = type;
        }

        public String type() {
            return type;
        }

        public EnforcementSupport support() {
            return EnforcementSupport.HARD_ONLY;
        }

        public void contribute(TimefoldBuildContext ctx, CompiledConstraint constraint) {
        }
    }

    static class TfObjective implements TimefoldObjectiveContributor {
        private final String type;

        TfObjective(String type) {
            this.type = type;
        }

        public String type() {
            return type;
        }

        public void contribute(TimefoldBuildContext ctx, CompiledObjective objective) {
        }
    }

    static class StubEngine implements SolverEngine {
        private final String name;

        StubEngine(String name) {
            this.name = name;
        }

        public String name() {
            return name;
        }

        public EngineCapabilities capabilities() {
            return new EngineCapabilities(name, Set.of(), java.util.Map.of(), Set.of(), false);
        }

        public SolveResult solve(CompiledProblem problem, SolveContext context) {
            throw new UnsupportedOperationException();
        }
    }

    static class OtherStubEngine extends StubEngine {
        OtherStubEngine(String name) {
            super(name);
        }
    }

    // ------------------------------------------------------------------ constraint families

    @Test
    void constraintFamilyLookupIsCaseInsensitiveAndSorted() {
        StubFamily rest = new StubFamily("min_rest");
        StubFamily overlap = new StubFamily("no_overlap");
        ConstraintFamilyRegistry registry = new ConstraintFamilyRegistry(List.of(overlap, rest));

        assertSame(rest, registry.get("min_rest"));
        assertSame(rest, registry.get("  MIN_Rest "));
        assertTrue(registry.contains("No_Overlap"));
        assertTrue(registry.find("nope").isEmpty());
        assertEquals(List.of("min_rest", "no_overlap"), List.copyOf(registry.supportedTypes()));
        assertThrows(UnsupportedOperationException.class, () -> registry.supportedTypes().add("x"));
    }

    @Test
    void unknownConstraintTypeIsRejectedWithTheDomainException() {
        ConstraintFamilyRegistry registry = new ConstraintFamilyRegistry(List.of(new StubFamily("min_rest")));
        assertThrows(UnknownConstraintException.class, () -> registry.get("nope"));
        assertThrows(UnknownConstraintException.class, () -> registry.get(null));
    }

    @Test
    void duplicateConstraintFamilyTypeFailsNamingBothClasses() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                new ConstraintFamilyRegistry(List.of(new StubFamily("min_rest"), new OtherStubFamily("MIN_REST"))));
        assertTrue(ex.getMessage().contains("min_rest"), ex.getMessage());
        assertTrue(ex.getMessage().contains(StubFamily.class.getName()), ex.getMessage());
        assertTrue(ex.getMessage().contains(OtherStubFamily.class.getName()), ex.getMessage());
    }

    @Test
    void blankConstraintFamilyTypeFails() {
        assertThrows(IllegalStateException.class,
                () -> new ConstraintFamilyRegistry(List.of(new StubFamily("  "))));
    }

    // ------------------------------------------------------------------ objective families

    @Test
    void objectiveFamilyLookupAndDuplicate() {
        StubObjective makespan = new StubObjective("minimize_makespan");
        ObjectiveFamilyRegistry registry = new ObjectiveFamilyRegistry(List.of(makespan));
        assertSame(makespan, registry.get("Minimize_Makespan"));
        assertEquals(List.of("minimize_makespan"), List.copyOf(registry.supportedTypes()));
        assertThrows(UnknownObjectiveException.class, () -> registry.get("nope"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                new ObjectiveFamilyRegistry(List.of(makespan, new StubObjective("minimize_makespan"))));
        assertTrue(ex.getMessage().contains("minimize_makespan"), ex.getMessage());
        assertTrue(ex.getMessage().contains(StubObjective.class.getName()), ex.getMessage());
    }

    // ------------------------------------------------------------------ contributors

    @Test
    void cpSatContributorLookupAndDuplicates() {
        CpConstraint rest = new CpConstraint("min_rest");
        CpObjective makespan = new CpObjective("minimize_makespan");
        CpSatContributorRegistry registry =
                new CpSatContributorRegistry(List.of(rest), List.of(makespan));

        assertSame(rest, registry.constraint("MIN_REST").orElseThrow());
        assertSame(makespan, registry.objective("minimize_makespan").orElseThrow());
        assertTrue(registry.constraint("nope").isEmpty());
        assertTrue(registry.objective(null).isEmpty());
        assertEquals(List.of("min_rest"), List.copyOf(registry.constraintTypes()));
        assertEquals(List.of("minimize_makespan"), List.copyOf(registry.objectiveTypes()));

        // the same type may exist once as constraint and once as objective, but not twice per kind
        assertNotNull(new CpSatContributorRegistry(List.of(new CpConstraint("x")), List.of(new CpObjective("x"))));
        IllegalStateException c = assertThrows(IllegalStateException.class, () ->
                new CpSatContributorRegistry(List.of(rest, new CpConstraint("min_rest")), List.of()));
        assertTrue(c.getMessage().contains(CpConstraint.class.getName()), c.getMessage());
        assertThrows(IllegalStateException.class, () ->
                new CpSatContributorRegistry(List.of(), List.of(makespan, new CpObjective("minimize_makespan"))));
    }

    @Test
    void timefoldContributorLookupAndDuplicates() {
        TfConstraint capacity = new TfConstraint("resource_capacity");
        TfObjective slotCost = new TfObjective("minimize_slot_cost");
        TimefoldContributorRegistry registry =
                new TimefoldContributorRegistry(List.of(capacity), List.of(slotCost));

        assertSame(capacity, registry.constraint("resource_capacity").orElseThrow());
        assertSame(slotCost, registry.objective("Minimize_Slot_Cost").orElseThrow());
        assertTrue(registry.constraint("min_rest").isEmpty());
        assertFalse(registry.objectiveTypes().isEmpty());

        assertThrows(IllegalStateException.class, () ->
                new TimefoldContributorRegistry(List.of(capacity, new TfConstraint("resource_capacity")), List.of()));
        assertThrows(IllegalStateException.class, () ->
                new TimefoldContributorRegistry(List.of(), List.of(slotCost, new TfObjective("minimize_slot_cost"))));
    }

    // ------------------------------------------------------------------ solver registry and aliases

    @Test
    void solverAliasesResolveToCanonicalIds() {
        StubEngine cpSat = new StubEngine("cp_sat");
        StubEngine timefold = new StubEngine("timefold");
        SolverRegistry registry = new SolverRegistry(List.of(timefold, cpSat));

        assertSame(cpSat, registry.get("cp_sat"));
        assertSame(cpSat, registry.get("cp-sat"));
        assertSame(cpSat, registry.get("CPSAT"));
        assertSame(cpSat, registry.get(" Cp-Sat "));
        assertSame(timefold, registry.get("TimeFold"));
        assertEquals("cp_sat", SolverRegistry.canonicalId("cp-sat"));
        assertEquals("cp_sat", SolverRegistry.canonicalId("cpsat"));
        assertEquals("timefold", SolverRegistry.canonicalId("Timefold"));
        assertEquals(List.of("cp_sat", "timefold"), List.copyOf(registry.supportedEngines()));
        assertTrue(registry.find("unknown").isEmpty());
        assertThrows(UnknownSolverException.class, () -> registry.get("unknown"));
        assertThrows(UnknownSolverException.class, () -> registry.get(null));
    }

    @Test
    void engineNamedWithAliasIsRegisteredUnderTheCanonicalId() {
        StubEngine cpSat = new StubEngine("cp-sat");
        SolverRegistry registry = new SolverRegistry(List.of(cpSat));
        assertSame(cpSat, registry.get("cp_sat"));
        assertEquals(List.of("cp_sat"), List.copyOf(registry.supportedEngines()));
    }

    @Test
    void duplicateEngineIdFailsNamingBothClasses() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                new SolverRegistry(List.of(new StubEngine("cp_sat"), new OtherStubEngine("cp-sat"))));
        assertTrue(ex.getMessage().contains(StubEngine.class.getName()), ex.getMessage());
        assertTrue(ex.getMessage().contains(OtherStubEngine.class.getName()), ex.getMessage());
    }

    // ------------------------------------------------------------------ capabilities

    @Test
    void capabilitiesReportUnsupportedTypesSorted() {
        EngineCapabilities caps = new EngineCapabilities(
                "timefold",
                Set.of(EngineCapabilities.Feature.SLOT_MODE),
                java.util.Map.of("resource_capacity", EnforcementSupport.HARD_ONLY),
                Set.of("minimize_slot_cost"),
                false);
        assertTrue(caps.supports(EngineCapabilities.Feature.SLOT_MODE));
        assertFalse(caps.supports(EngineCapabilities.Feature.FREE_START));
        assertEquals(List.of("min_rest", "no_overlap"),
                List.copyOf(caps.unsupportedConstraintTypes(List.of("no_overlap", "resource_capacity", "min_rest"))));
        assertEquals(List.of("minimize_makespan"),
                List.copyOf(caps.unsupportedObjectiveTypes(List.of("minimize_slot_cost", "minimize_makespan"))));
    }
}
