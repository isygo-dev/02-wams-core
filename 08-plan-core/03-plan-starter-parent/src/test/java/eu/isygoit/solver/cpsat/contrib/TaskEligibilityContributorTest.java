package eu.isygoit.solver.cpsat.contrib;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.ConstraintProto;
import com.google.ortools.sat.Literal;
import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.Slot;
import eu.isygoit.dto.request.params.TaskEligibilityParams;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.contrib.ContribTestSupport.FakeEnforcer;
import eu.isygoit.solver.family.TaskEligibilityFamily;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Builds the model with a stub {@code CpModelContext} (no native library) and inspects the generated proto. */
class TaskEligibilityContributorTest {

    private final TaskEligibilityContributor contributor = new TaskEligibilityContributor();
    private final TaskEligibilityFamily family = new TaskEligibilityFamily();

    /** One task with three candidates on r0 (allowed), r1 and r2 (forbidden by resourceIds=[r0]). */
    private ContribTestSupport fixture() {
        ContribTestSupport s = new ContribTestSupport(3);
        s.task(0, 50, false, 0, 5, 1, 5, 2, 5);
        return s;
    }

    private CompiledConstraint resourceConstraint(ContribTestSupport s, IEnumEnforcement.Types enforcement) {
        Constraint c = ContribTestSupport.constraint("task_eligibility", enforcement, ConstraintScope.all());
        TaskEligibilityParams p = new TaskEligibilityParams(List.of("r0"), null, null, null, null);
        when(s.index.resolveTasks(any())).thenReturn(new int[]{0});
        return ContribTestSupport.compiled(c, family, p);
    }

    @Test
    void identityAndSupport() {
        assertEquals("task_eligibility", contributor.type());
        assertTrue(contributor.support().hard() && contributor.support().soft() && contributor.support().relaxable());
    }

    @Test
    void hardSolveAddsNothingBecausePreprocessAlreadyPrunedTheIndex() {
        ContribTestSupport s = fixture();
        CompiledConstraint cc = resourceConstraint(s, IEnumEnforcement.Types.STRICTLY_REQUIRED);
        FakeEnforcer enforcer = s.hard();
        contributor.apply(s.ctx, cc, enforcer);
        assertTrue(s.constraints().isEmpty());
        assertEquals(0, enforcer.instanceCalls);
    }

    @Test
    void softForbidsEachNotAllowedCandidateWithItsOwnInstanceLiteral() {
        ContribTestSupport s = fixture();
        CompiledConstraint cc = resourceConstraint(s, IEnumEnforcement.Types.SOFT);
        FakeEnforcer enforcer = s.soft();
        contributor.apply(s.ctx, cc, enforcer);

        List<ConstraintProto> clauses = s.constraints(ConstraintProto.ConstraintCase.BOOL_OR);
        assertEquals(2, clauses.size()); // r1 and r2 only: the allowed r0 candidate is untouched
        assertEquals(2, enforcer.instanceCalls); // soft unit = one forbidden (task, resource) used
        for (int k = 0; k < 2; k++) {
            ConstraintProto clause = clauses.get(k);
            assertEquals(1, clause.getBoolOr().getLiteralsCount());
            assertEquals(s.candidateLiteral(k + 1).not().getIndex(), clause.getBoolOr().getLiterals(0));
            assertEquals(1, clause.getEnforcementLiteralCount());
            assertEquals(enforcer.issued.get(k).getIndex(), clause.getEnforcementLiteral(0));
        }
    }

    @Test
    void relaxableGuardsEveryClauseWithTheSharedActiveLiteral() {
        ContribTestSupport s = fixture();
        BoolVar active = s.model.newBoolVar("active");
        contributor.apply(s.ctx, resourceConstraint(s, IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION), s.relaxable(active));
        List<ConstraintProto> clauses = s.constraints(ConstraintProto.ConstraintCase.BOOL_OR);
        assertEquals(2, clauses.size());
        for (ConstraintProto clause : clauses) {
            assertEquals(List.of(active.getIndex()), clause.getEnforcementLiteralList());
        }
    }

    @Test
    void explainPassGuardsTheHardConstraintWithItsAssumptionLiteral() {
        ContribTestSupport s = fixture();
        s.explain(true);
        BoolVar assumption = s.model.newBoolVar("assumption");
        contributor.apply(s.ctx, resourceConstraint(s, IEnumEnforcement.Types.STRICTLY_REQUIRED), s.assumption(assumption));
        List<ConstraintProto> clauses = s.constraints(ConstraintProto.ConstraintCase.BOOL_OR);
        assertEquals(2, clauses.size());
        for (ConstraintProto clause : clauses) {
            assertEquals(List.of(assumption.getIndex()), clause.getEnforcementLiteralList());
        }
    }

    @Test
    void slotRestrictionOnAFreeStartTaskUsesEqualityLiteralsAndOneInstance() {
        ContribTestSupport s = fixture();
        s.slots.add(new Slot("s1", "p", "s1", ContribTestSupport.T0, ContribTestSupport.T0.plusSeconds(600), 0.0, null));
        s.slots.add(new Slot("s2", "p", "s2", ContribTestSupport.T0.plusSeconds(1200), ContribTestSupport.T0.plusSeconds(1800), 0.0, null));
        Constraint c = ContribTestSupport.constraint("task_eligibility", IEnumEnforcement.Types.SOFT, ConstraintScope.all());
        TaskEligibilityParams p = new TaskEligibilityParams(null, null, null, null, List.of("s2")); // allowed start: tick 20
        when(s.index.resolveTasks(any())).thenReturn(new int[]{0});
        FakeEnforcer enforcer = s.soft();

        contributor.apply(s.ctx, ContribTestSupport.compiled(c, family, p), enforcer);

        assertEquals(1, enforcer.instanceCalls);
        assertEquals(1, s.constraints(ConstraintProto.ConstraintCase.LINEAR).size()); // eq => start == 20 (definitional)
        List<ConstraintProto> clauses = s.constraints(ConstraintProto.ConstraintCase.BOOL_OR);
        assertEquals(1, clauses.size());
        assertEquals(1, clauses.get(0).getBoolOr().getLiteralsCount());
        assertEquals(List.of(enforcer.issued.get(0).getIndex()), clauses.get(0).getEnforcementLiteralList());
    }

    @Test
    void slotRestrictionOnASlotModeTaskForbidsTheStartChoicesOutsideTheAllowedSlots() {
        ContribTestSupport s = fixture();
        s.slots.add(new Slot("s1", "p", "s1", ContribTestSupport.T0, ContribTestSupport.T0.plusSeconds(600), 0.0, null));
        s.slots.add(new Slot("s2", "p", "s2", ContribTestSupport.T0.plusSeconds(1200), ContribTestSupport.T0.plusSeconds(1800), 0.0, null));
        BoolVar y0 = s.model.newBoolVar("y0");
        BoolVar y1 = s.model.newBoolVar("y1");
        when(s.ctx.startChoice(0)).thenReturn(new Literal[]{y0, y1});
        when(s.ctx.startChoiceValues(0)).thenReturn(new long[]{0, 20});
        Constraint c = ContribTestSupport.constraint("task_eligibility", IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION, ConstraintScope.all());
        TaskEligibilityParams p = new TaskEligibilityParams(null, null, null, null, List.of("s2"));
        when(s.index.resolveTasks(any())).thenReturn(new int[]{0});
        BoolVar active = s.model.newBoolVar("active");

        contributor.apply(s.ctx, ContribTestSupport.compiled(c, family, p), s.relaxable(active));

        List<ConstraintProto> clauses = s.constraints(ConstraintProto.ConstraintCase.BOOL_OR);
        assertEquals(1, clauses.size()); // only the choice for start 0 is outside the allowed slot starts
        assertEquals(y0.not().getIndex(), clauses.get(0).getBoolOr().getLiterals(0));
        assertEquals(List.of(active.getIndex()), clauses.get(0).getEnforcementLiteralList());
    }

    @Test
    void anOptionalTaskSlotRestrictionIsConditionedOnItsPresence() {
        ContribTestSupport s = new ContribTestSupport(1);
        s.task(0, 50, true, 0, 5);
        s.slots.add(new Slot("s2", "p", "s2", ContribTestSupport.T0.plusSeconds(1200), ContribTestSupport.T0.plusSeconds(1800), 0.0, null));
        Constraint c = ContribTestSupport.constraint("task_eligibility", IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION, ConstraintScope.all());
        TaskEligibilityParams p = new TaskEligibilityParams(null, null, null, null, List.of("s2"));
        when(s.index.resolveTasks(any())).thenReturn(new int[]{0});
        BoolVar active = s.model.newBoolVar("active");

        contributor.apply(s.ctx, ContribTestSupport.compiled(c, family, p), s.relaxable(active));

        ConstraintProto clause = s.constraints(ConstraintProto.ConstraintCase.BOOL_OR).get(0);
        assertEquals(2, clause.getEnforcementLiteralCount()); // active + presence
    }
}
