package eu.isygoit.solver.cpsat.contrib;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.ConstraintProto;
import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.TimeWindow;
import eu.isygoit.dto.request.params.ResourceAvailabilityParams;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.contrib.ContribTestSupport.FakeEnforcer;
import eu.isygoit.solver.family.ResourceAvailabilityFamily;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Instance-level formulation: one instance literal per candidate overlapping blocked time. */
class ResourceAvailabilityContributorTest {

    private final ResourceAvailabilityContributor contributor = new ResourceAvailabilityContributor();
    private final ResourceAvailabilityFamily family = new ResourceAvailabilityFamily();

    private static TimeWindow window(int from, int to) {
        return new TimeWindow(ContribTestSupport.T0.plusSeconds(from * 60L), ContribTestSupport.T0.plusSeconds(to * 60L));
    }

    /** One task (start domain [est, lst]) with one candidate of the given duration on r0; r1 is out of scope. */
    private ContribTestSupport fixture(int est, int lst, int duration) {
        ContribTestSupport s = new ContribTestSupport(2);
        s.task(est, lst, false, 0, duration, 1, duration);
        when(s.index.resolveResources(any())).thenReturn(new int[]{0});
        return s;
    }

    private CompiledConstraint compiled(IEnumEnforcement.Types e, TimeWindow... windows) {
        Constraint c = ContribTestSupport.constraint("resource_availability", e, ConstraintScope.all());
        return ContribTestSupport.compiled(c, family, new ResourceAvailabilityParams(List.of(windows), null));
    }

    @Test
    void identity() {
        assertEquals("resource_availability", contributor.type());
        assertTrue(contributor.support().hard() && contributor.support().soft() && contributor.support().relaxable());
    }

    @Test
    void hardSolveAddsNothingBecausePreprocessAlreadyRestrictedTheCandidateStarts() {
        ContribTestSupport s = fixture(0, 90, 5);
        FakeEnforcer enforcer = s.hard();
        contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.STRICTLY_REQUIRED, window(50, 60)), enforcer);
        assertTrue(s.constraints().isEmpty());
        assertEquals(0, enforcer.instanceCalls);
    }

    @Test
    void softUsesOneInstanceLiteralPerCandidateWhateverTheNumberOfBlockedWindows() {
        ContribTestSupport s = fixture(0, 90, 5);
        FakeEnforcer enforcer = s.soft();
        contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.SOFT, window(50, 60), window(70, 80)), enforcer);

        assertEquals(1, enforcer.instanceCalls); // the candidate on r0 only: r1 is out of scope
        List<ConstraintProto> clauses = s.constraints(ConstraintProto.ConstraintCase.BOOL_OR);
        assertEquals(2, clauses.size()); // one clause per reachable forbidden start run
        for (ConstraintProto clause : clauses) {
            assertEquals(3, clause.getBoolOr().getLiteralsCount()); // not cand, before, after
            assertEquals(List.of(enforcer.issued.get(0).getIndex()), clause.getEnforcementLiteralList());
        }
        // before => start <= a-1 and after => start >= b+1, both definitional (enforced by their own literal only)
        List<ConstraintProto> linear = s.constraints(ConstraintProto.ConstraintCase.LINEAR);
        assertEquals(4, linear.size());
        for (ConstraintProto c : linear) {
            assertEquals(1, c.getEnforcementLiteralCount());
        }
    }

    @Test
    void aWindowAtTheStartOfTheDomainLeavesOnlyTheAfterLiteral() {
        ContribTestSupport s = fixture(0, 90, 5);
        contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.SOFT, window(0, 10)), s.soft());
        ConstraintProto clause = s.constraints(ConstraintProto.ConstraintCase.BOOL_OR).get(0);
        assertEquals(2, clause.getBoolOr().getLiteralsCount()); // forbidden starts [0, 9]: before is impossible
        assertEquals(1, s.constraints(ConstraintProto.ConstraintCase.LINEAR).size());
    }

    @Test
    void aCandidateWhoseDomainAlreadyAvoidsBlockedTimeGetsNothing() {
        ContribTestSupport s = fixture(0, 40, 5); // latest end 45 < 50
        FakeEnforcer enforcer = s.soft();
        contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.SOFT, window(50, 60)), enforcer);
        assertEquals(0, enforcer.instanceCalls);
        assertTrue(s.constraints().isEmpty());
    }

    @Test
    void zeroDurationCandidatesOccupyNothingAndAreSkipped() {
        ContribTestSupport s = fixture(0, 90, 0);
        FakeEnforcer enforcer = s.soft();
        contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.SOFT, window(50, 60)), enforcer);
        assertEquals(0, enforcer.instanceCalls);
        assertTrue(s.constraints().isEmpty());
    }

    @Test
    void relaxableAndExplainGuardTheClauseWithTheSharedLiteral() {
        ContribTestSupport s = fixture(0, 90, 5);
        BoolVar active = s.model.newBoolVar("active");
        contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION, window(50, 60)), s.relaxable(active));
        ConstraintProto clause = s.constraints(ConstraintProto.ConstraintCase.BOOL_OR).get(0);
        assertEquals(List.of(active.getIndex()), clause.getEnforcementLiteralList());

        ContribTestSupport e = fixture(0, 90, 5);
        e.explain(true);
        BoolVar assumption = e.model.newBoolVar("assumption");
        contributor.apply(e.ctx, compiled(IEnumEnforcement.Types.STRICTLY_REQUIRED, window(50, 60)), e.assumption(assumption));
        ConstraintProto explained = e.constraints(ConstraintProto.ConstraintCase.BOOL_OR).get(0);
        assertEquals(List.of(assumption.getIndex()), explained.getEnforcementLiteralList());
    }
}
