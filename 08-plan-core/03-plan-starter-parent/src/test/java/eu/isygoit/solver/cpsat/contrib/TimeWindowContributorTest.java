package eu.isygoit.solver.cpsat.contrib;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.ConstraintProto;
import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.params.TimeWindowParams;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.contrib.ContribTestSupport.FakeEnforcer;
import eu.isygoit.solver.family.TimeWindowFamily;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** HARD is a preprocess prune, SOFT charges the slack in ticks, RELAXABLE / explain use the plain enforced bound. */
class TimeWindowContributorTest {

    private final TimeWindowContributor contributor = new TimeWindowContributor();
    private final TimeWindowFamily family = new TimeWindowFamily();

    private static Instant at(int tick) {
        return ContribTestSupport.T0.plusSeconds(tick * 60L);
    }

    private ContribTestSupport fixture(boolean optional, int... resourceAndDuration) {
        ContribTestSupport s = new ContribTestSupport(2);
        s.task(0, 100, optional, resourceAndDuration);
        when(s.index.resolveTasks(any())).thenReturn(new int[]{0});
        return s;
    }

    private CompiledConstraint compiled(IEnumEnforcement.Types e, TimeWindowParams p) {
        Constraint c = ContribTestSupport.constraint("time_window", e, ConstraintScope.all());
        return ContribTestSupport.compiled(c, family, p);
    }

    @Test
    void identity() {
        assertEquals("time_window", contributor.type());
        assertTrue(contributor.support().hard() && contributor.support().soft() && contributor.support().relaxable());
    }

    @Test
    void hardSolveAddsNothingForAFixedDurationBecausePreprocessIsExact() {
        ContribTestSupport s = fixture(false, 0, 10);
        contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.STRICTLY_REQUIRED, new TimeWindowParams(at(10), at(50), at(20), at(60))), s.hard());
        assertTrue(s.constraints().isEmpty());
    }

    @Test
    void hardSolveAddsTheExactEndBoundsWhenCandidateDurationsDiffer() {
        ContribTestSupport s = fixture(false, 0, 4, 1, 10);
        FakeEnforcer enforcer = s.hard();
        contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.STRICTLY_REQUIRED, new TimeWindowParams(at(10), null, at(20), at(60))), enforcer);
        List<ConstraintProto> linear = s.constraints(ConstraintProto.ConstraintCase.LINEAR);
        assertEquals(2, linear.size()); // earliestEnd and latestEnd; the start bound stays with the preprocess
        for (ConstraintProto c : linear) {
            assertEquals(0, c.getEnforcementLiteralCount());
        }
    }

    @Test
    void softPenalisesOneSlackPerBoundInTicks() {
        ContribTestSupport s = fixture(false, 0, 10);
        FakeEnforcer enforcer = s.soft();
        contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.SOFT, new TimeWindowParams(at(10), null, null, at(60))), enforcer);
        assertEquals(2, enforcer.penalized.size()); // earliestStart + latestEnd
        assertEquals(0, enforcer.instanceCalls, "a measure is penalised, no unit literal is requested");
        List<ConstraintProto> linear = s.constraints(ConstraintProto.ConstraintCase.LINEAR);
        assertEquals(2, linear.size());
        for (ConstraintProto c : linear) {
            assertEquals(0, c.getEnforcementLiteralCount()); // required task: the measure channel needs no condition
        }
    }

    @Test
    void softChannelOfAnOptionalTaskIsConditionedOnItsPresence() {
        ContribTestSupport s = fixture(true, 0, 10);
        contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.SOFT, new TimeWindowParams(at(10), null, null, null)), s.soft());
        List<ConstraintProto> linear = s.constraints(ConstraintProto.ConstraintCase.LINEAR);
        assertEquals(1, linear.size());
        assertEquals(1, linear.get(0).getEnforcementLiteralCount());
    }

    @Test
    void relaxableGuardsEveryBoundWithTheSharedLiteralAndPresence() {
        ContribTestSupport s = fixture(true, 0, 10);
        BoolVar active = s.model.newBoolVar("active");
        contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION, new TimeWindowParams(at(10), at(50), null, null)),
                s.relaxable(active));
        List<ConstraintProto> linear = s.constraints(ConstraintProto.ConstraintCase.LINEAR);
        assertEquals(2, linear.size());
        for (ConstraintProto c : linear) {
            assertEquals(2, c.getEnforcementLiteralCount()); // active + presence of the optional task
            assertEquals(active.getIndex(), c.getEnforcementLiteral(0));
        }
    }

    @Test
    void explainPassEnforcesAllFourBoundsWithTheAssumptionLiteral() {
        ContribTestSupport s = fixture(false, 0, 10);
        s.explain(true);
        BoolVar assumption = s.model.newBoolVar("assumption");
        contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.STRICTLY_REQUIRED, new TimeWindowParams(at(10), at(50), at(20), at(60))),
                s.assumption(assumption));
        List<ConstraintProto> linear = s.constraints(ConstraintProto.ConstraintCase.LINEAR);
        assertEquals(4, linear.size());
        for (ConstraintProto c : linear) {
            assertEquals(List.of(assumption.getIndex()), c.getEnforcementLiteralList());
        }
    }
}
