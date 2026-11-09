package eu.isygoit.solver.cpsat.contrib;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.ConstraintProto;
import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.PrecedenceRef;
import eu.isygoit.dto.request.params.PrecedenceParams;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.exception.InvalidRequestException;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.contrib.ContribTestSupport.FakeEnforcer;
import eu.isygoit.solver.family.PrecedenceFamily;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** One instance literal per (from, to) pair shared by the min-lag and max-lag constraints. */
class PrecedenceContributorTest {

    private final PrecedenceContributor contributor = new PrecedenceContributor();
    private final PrecedenceFamily family = new PrecedenceFamily();

    private static final ConstraintScope FROM = new ConstraintScope("tag", "from");
    private static final ConstraintScope TO = new ConstraintScope("tag", "to");

    /** tasks 0,1 = "from" side, tasks 2,3 = "to" side; task 3 optional when requested. */
    private ContribTestSupport fixture(boolean optionalLast) {
        ContribTestSupport s = new ContribTestSupport(1);
        s.task(0, 50, false, 0, 5);
        s.task(0, 50, false, 0, 5);
        s.task(0, 50, false, 0, 5);
        s.task(0, 50, optionalLast, 0, 5);
        when(s.index.resolveTasks(any())).thenAnswer(i -> {
            ConstraintScope scope = i.getArgument(0);
            return "from".equals(scope.value()) ? new int[]{0, 1} : new int[]{2, 3};
        });
        return s;
    }

    private CompiledConstraint compiled(IEnumEnforcement.Types e, PrecedenceParams p) {
        Constraint c = ContribTestSupport.constraint("precedence", e, ConstraintScope.all());
        return ContribTestSupport.compiled(c, family, p);
    }

    private static PrecedenceParams params(Integer minLag, Integer maxLag, PrecedenceParams.PairingMode pairing) {
        return new PrecedenceParams(FROM, TO, PrecedenceRef.Type.FINISH_START, minLag, maxLag, pairing);
    }

    @Test
    void identity() {
        assertEquals("precedence", contributor.type());
        assertTrue(contributor.support().hard() && contributor.support().soft() && contributor.support().relaxable());
    }

    @Test
    void hardEmitsAnUnconditionalLinearConstraintPerPair() {
        ContribTestSupport s = fixture(false);
        FakeEnforcer enforcer = s.hard();
        contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.STRICTLY_REQUIRED, params(3, null, null)), enforcer);
        List<ConstraintProto> linear = s.constraints(ConstraintProto.ConstraintCase.LINEAR);
        assertEquals(4, linear.size()); // 2 x 2 pairs
        for (ConstraintProto c : linear) {
            assertEquals(0, c.getEnforcementLiteralCount()); // all tasks required: truly unconditional
        }
        assertEquals(4, enforcer.instanceCalls);
    }

    @Test
    void aMaxLagAddsASecondConstraintOnTheSameInstanceLiteral() {
        ContribTestSupport s = fixture(false);
        FakeEnforcer enforcer = s.soft();
        contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.SOFT, params(0, 10, PrecedenceParams.PairingMode.BY_ORDER)), enforcer);
        List<ConstraintProto> linear = s.constraints(ConstraintProto.ConstraintCase.LINEAR);
        assertEquals(4, linear.size()); // BY_ORDER: 2 pairs x (min + max)
        assertEquals(2, enforcer.instanceCalls); // soft unit = one violated PAIR whichever lag is broken
        for (int k = 0; k < 2; k++) {
            int literal = enforcer.issued.get(k).getIndex();
            assertEquals(List.of(literal), linear.get(2 * k).getEnforcementLiteralList());
            assertEquals(List.of(literal), linear.get(2 * k + 1).getEnforcementLiteralList());
        }
    }

    @Test
    void anOptionalEndpointAddsItsPresenceToTheConditions() {
        ContribTestSupport s = fixture(true);
        contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.STRICTLY_REQUIRED, params(0, null, PrecedenceParams.PairingMode.BY_ORDER)),
                s.hard());
        List<ConstraintProto> linear = s.constraints(ConstraintProto.ConstraintCase.LINEAR);
        assertEquals(2, linear.size());
        assertEquals(0, linear.get(0).getEnforcementLiteralCount()); // pair (0,2): both required
        assertEquals(1, linear.get(1).getEnforcementLiteralCount()); // pair (1,3): task 3 optional
    }

    @Test
    void relaxableAndExplainShareOneLiteralAcrossAllPairs() {
        ContribTestSupport s = fixture(false);
        BoolVar active = s.model.newBoolVar("active");
        contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION, params(0, null, null)), s.relaxable(active));
        for (ConstraintProto c : s.constraints(ConstraintProto.ConstraintCase.LINEAR)) {
            assertEquals(List.of(active.getIndex()), c.getEnforcementLiteralList());
        }
        ContribTestSupport e = fixture(false);
        e.explain(true);
        BoolVar assumption = e.model.newBoolVar("assumption");
        contributor.apply(e.ctx, compiled(IEnumEnforcement.Types.STRICTLY_REQUIRED, params(0, null, null)), e.assumption(assumption));
        assertEquals(4, e.constraints(ConstraintProto.ConstraintCase.LINEAR).size());
        for (ConstraintProto c : e.constraints(ConstraintProto.ConstraintCase.LINEAR)) {
            assertEquals(List.of(assumption.getIndex()), c.getEnforcementLiteralList());
        }
    }

    @Test
    void tooManyPairsIsAnErrorInTheSolvePassAndSkippedInTheExplainPass() {
        ContribTestSupport s = fixture(false);
        s.maxPairwise(3);
        InvalidRequestException e = assertThrows(InvalidRequestException.class,
                () -> contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.STRICTLY_REQUIRED, params(0, null, null)), s.hard()));
        assertEquals("PAIRWISE_TOO_LARGE", e.code());

        ContribTestSupport x = fixture(false);
        x.maxPairwise(3);
        x.explain(true);
        contributor.apply(x.ctx, compiled(IEnumEnforcement.Types.STRICTLY_REQUIRED, params(0, null, null)), x.assumption(x.model.newBoolVar("a")));
        assertTrue(x.constraints().isEmpty());
    }
}
