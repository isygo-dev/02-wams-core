package eu.isygoit.solver.cpsat;

import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.Literal;
import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.RelaxationPolicy;
import eu.isygoit.dto.request.SolverOptions;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.CompiledConstraint;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Enforcer semantics of design 5.5 / 6.9 / 6.10 (HARD, RELAXABLE, SOFT, explain assumption) and the soft literal
 * accounting, on a plain {@link CpModel} (no solve, no CandidateIndex). Skipped when the OR-Tools model classes cannot be
 * loaded.
 */
class EnforcerTest {

    private CpModel model;
    private TierObjectiveBuilder tiers;
    private AtomicInteger boolVars;

    @BeforeEach
    void setUp() {
        boolean usable;
        try {
            model = new CpModel();
            model.newBoolVar("probe");
            usable = true;
        } catch (Throwable unusable) {
            usable = false;
        }
        Assumptions.assumeTrue(usable, "OR-Tools model classes not loadable");
        tiers = new TierObjectiveBuilder(8);
        boolVars = new AtomicInteger();
    }

    private CpSatEnforcerFactory solveFactory(SolverOptions options) {
        return new CpSatEnforcerFactory(model, tiers, null, options, boolVars::addAndGet);
    }

    private static CompiledConstraint compiled(int index, String id, IEnumEnforcement.Types enforcement, int weight,
                                               Integer tier, RelaxationPolicy relaxation) {
        Constraint constraint = new Constraint(id, "min_rest", enforcement, null, Map.of(), relaxation, weight, tier);
        return new CompiledConstraint(index, constraint, null, null, true);
    }

    @Test
    void hardInTheSolvePassIsUnconditional() {
        CpSatEnforcerFactory factory = solveFactory(SolverOptions.defaults());
        Enforcer enforcer = factory.create(compiled(0, "c", IEnumEnforcement.Types.STRICTLY_REQUIRED, 0, null, null));
        assertEquals(Enforcer.Mode.HARD, enforcer.mode());
        assertNull(enforcer.instance());
        assertNull(enforcer.instance(3, 4));
        assertFalse(enforcer.penalizeMeasure(LinearExpr.constant(1), 1, 0), "HARD never penalises a measure");
        assertTrue(tiers.isEmpty());
    }

    @Test
    void configurableHardBehavesLikeHard() {
        Enforcer enforcer = solveFactory(SolverOptions.defaults())
                .create(compiled(0, "c", IEnumEnforcement.Types.CONFIGURABLE_HARD, 0, null, null));
        assertEquals(Enforcer.Mode.HARD, enforcer.mode());
        assertNull(enforcer.instance());
    }

    @Test
    void hardInTheExplainPassUsesOneSharedAssumptionLiteralPerConstraint() {
        AssumptionRegistry registry = new AssumptionRegistry(model);
        CpSatEnforcerFactory factory = new CpSatEnforcerFactory(model, tiers, registry, SolverOptions.defaults(), null);
        Enforcer first = factory.create(compiled(0, "rest", IEnumEnforcement.Types.STRICTLY_REQUIRED, 0, null, null));
        Enforcer second = factory.create(compiled(1, "cap", IEnumEnforcement.Types.STRICTLY_REQUIRED, 0, null, null));
        Literal rest = first.instance();
        assertNotNull(rest);
        assertSame(rest, first.instance(5, 6), "every instance of a constraint shares the assumption literal");
        assertNotSame(rest, second.instance());
        assertEquals(Enforcer.Mode.HARD, first.mode());
        assertEquals(2, registry.size());
        assertEquals(AssumptionRegistry.KIND_CONSTRAINT, registry.refs().get(0).kind());
        assertEquals("rest", registry.refs().get(0).id());
    }

    @Test
    void softCreatesOneSatisfiedLiteralPerInstanceAndAccountsForIt() {
        CpSatEnforcerFactory factory = solveFactory(SolverOptions.defaults());
        Enforcer enforcer = factory.create(compiled(0, "soft", IEnumEnforcement.Types.SOFT, 5, 2, null));
        assertEquals(Enforcer.Mode.SOFT, enforcer.mode());
        Literal a = enforcer.instance(1, 2);
        Literal again = enforcer.instance(1, 2);
        Literal b = enforcer.instance(3, -1);
        assertNotNull(a);
        assertSame(a, again, "the same (task, resource) instance reuses its literal");
        assertNotSame(a, b);
        assertEquals(2, factory.softInstances().size());
        assertEquals(2, boolVars.get(), "each new literal is reported to the size guard");
        CpSatEnforcerFactory.SoftInstance first = factory.softInstances().get(0);
        assertEquals("soft", first.constraintId());
        assertEquals(1, first.taskIdx());
        assertEquals(2, first.resourceIdx());
        assertEquals(5, first.weight());
        assertEquals(2, first.tier());
        // two unit penalties of weight 5 in tier 2: range [0, 10]
        assertArrayEquals(new int[]{2}, tiers.tiers());
        TierObjectiveBuilder.Plan plan = tiers.plan(null);
        assertArrayEquals(new long[]{0}, plan.lows());
        assertArrayEquals(new long[]{10}, plan.highs());
    }

    @Test
    void softWeightZeroBecomesOneInTheConstraintRecord() {
        Constraint constraint = new Constraint("w", "min_rest", IEnumEnforcement.Types.SOFT, null, Map.of(), null, 0, null);
        assertEquals(1, constraint.weight());
        assertEquals(0, constraint.tier().intValue());
    }

    @Test
    void softMeasureIsPenalisedWithWeightTimesTheMaximum() {
        CpSatEnforcerFactory factory = solveFactory(SolverOptions.defaults());
        Enforcer enforcer = factory.create(compiled(0, "soft", IEnumEnforcement.Types.SOFT, 3, 1, null));
        assertTrue(enforcer.penalizeMeasure(LinearExpr.constant(2), 7, 4));
        assertEquals(1, factory.softMeasureCount());
        TierObjectiveBuilder.Plan plan = tiers.plan(null);
        assertArrayEquals(new int[]{1}, plan.tiers());
        assertArrayEquals(new long[]{21}, plan.highs());
        assertEquals(1, factory.softInstances().size());
        assertNull(factory.softInstances().get(0).satisfied(), "measure penalties have no unit literal");
    }

    @Test
    void relaxableConstraintsShareOneActiveLiteralAndCostInTierMinusTwo() {
        CpSatEnforcerFactory factory = solveFactory(SolverOptions.defaults());
        Constraint probe = compiled(0, "deadline", IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION, 0, null,
                new RelaxationPolicy(true, 5, 1.0d)).constraint();
        assertTrue(CpSatEnforcerFactory.isRelaxable(probe, SolverOptions.defaults()));
        assertEquals(6000L, CpSatEnforcerFactory.relaxationCost(probe));

        Enforcer enforcer = factory.create(compiled(0, "deadline", IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION,
                0, null, new RelaxationPolicy(true, 5, 1.0d)));
        assertEquals(Enforcer.Mode.RELAXABLE, enforcer.mode());
        Literal active = enforcer.instance();
        assertNotNull(active);
        assertSame(active, enforcer.instance(1, 1));
        assertFalse(enforcer.penalizeMeasure(LinearExpr.constant(1), 1, 0));
        assertEquals(1, factory.relaxLiterals().size());
        assertEquals(6000L, factory.relaxLiterals().get(0).cost());
        assertArrayEquals(new int[]{TierObjectiveBuilder.TIER_RELAXATION}, tiers.tiers());
        assertArrayEquals(new long[]{6000L}, tiers.plan(null).highs());
    }

    @Test
    void relaxableInTheExplainPassIsFreeAndCostsNothing() {
        AssumptionRegistry registry = new AssumptionRegistry(model);
        CpSatEnforcerFactory factory = new CpSatEnforcerFactory(model, tiers, registry, SolverOptions.defaults(), null);
        Enforcer enforcer = factory.create(compiled(0, "deadline", IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION,
                0, null, new RelaxationPolicy(true, 1, 1.0d)));
        assertEquals(Enforcer.Mode.RELAXABLE, enforcer.mode());
        assertNotNull(enforcer.instance());
        assertEquals(0L, factory.relaxLiterals().get(0).cost());
        assertTrue(tiers.isEmpty());
        assertEquals(0, registry.size(), "relaxation literals are not assumptions");
    }

    @Test
    void unauthorizedOrExcludedRelaxableConstraintsAreHard() {
        Constraint unauthorized = compiled(0, "u", IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION, 0, null,
                new RelaxationPolicy(false, 0, 1.0d)).constraint();
        assertFalse(CpSatEnforcerFactory.isRelaxable(unauthorized, SolverOptions.defaults()));
        Constraint noRelaxation = compiled(0, "n", IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION, 0, null,
                new RelaxationPolicy(true, 0, 0.0d)).constraint();
        assertFalse(CpSatEnforcerFactory.isRelaxable(noRelaxation, SolverOptions.defaults()));

        SolverOptions onlyOther = new SolverOptions(null, null, null, null, null, List.of("other"), null, null, null, null,
                null, null, null);
        Constraint listed = compiled(0, "d", IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION, 0, null,
                new RelaxationPolicy(true, 0, 1.0d)).constraint();
        assertFalse(CpSatEnforcerFactory.isRelaxable(listed, onlyOther));
        Enforcer enforcer = solveFactory(onlyOther).create(compiled(0, "d",
                IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION, 0, null, new RelaxationPolicy(true, 0, 1.0d)));
        assertEquals(Enforcer.Mode.HARD, enforcer.mode());
        assertNull(enforcer.instance());
    }

    @Test
    void softConstraintsAreNotBuiltInTheExplainPass() {
        CpSatEnforcerFactory factory = new CpSatEnforcerFactory(model, tiers, new AssumptionRegistry(model),
                SolverOptions.defaults(), null);
        CompiledConstraint soft = compiled(0, "soft", IEnumEnforcement.Types.SOFT, 1, null, null);
        assertThrows(IllegalStateException.class, () -> factory.create(soft));
    }

    @Test
    void conditionsCombineTheInstanceWithTheExtrasAndSkipNulls() {
        Enforcer hard = solveFactory(SolverOptions.defaults())
                .create(compiled(0, "c", IEnumEnforcement.Types.STRICTLY_REQUIRED, 0, null, null));
        Literal extra = model.newBoolVar("extra");
        assertEquals(0, hard.conditions(null).length, "unconditional: empty array");
        assertEquals(0, hard.conditions(null, (Literal) null).length);
        Literal[] onlyExtra = hard.conditions(null, null, extra);
        assertEquals(1, onlyExtra.length);
        assertSame(extra, onlyExtra[0]);

        Enforcer soft = solveFactory(SolverOptions.defaults())
                .create(compiled(1, "s", IEnumEnforcement.Types.SOFT, 1, null, null));
        Literal instance = soft.instance(0, 0);
        Literal[] both = soft.conditions(instance, extra);
        assertEquals(2, both.length);
        assertSame(instance, both[0]);
        assertSame(extra, both[1]);
    }
}
