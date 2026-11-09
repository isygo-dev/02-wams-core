package eu.isygoit.solver.cpsat.contrib;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.ConstraintProto;
import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.params.NoOverlapParams;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.exception.InvalidRequestException;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.contrib.ContribTestSupport.FakeEnforcer;
import eu.isygoit.solver.family.NoOverlapFamily;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Universal contributor rule: global NoOverlap when unconditional, pairwise literals otherwise. */
class NoOverlapContributorTest {

    private final NoOverlapContributor contributor = new NoOverlapContributor();
    private final NoOverlapFamily family = new NoOverlapFamily();

    /** Three free tasks of duration 5 in one group. */
    private ContribTestSupport fixture() {
        ContribTestSupport s = new ContribTestSupport(1);
        s.task(0, 50, false, 0, 5);
        s.task(0, 50, false, 0, 5);
        s.task(0, 50, false, 0, 5);
        when(s.index.groupTasksByTagPrefix("class-")).thenReturn(new int[][]{{0, 1, 2}});
        when(s.ctx.masterInterval(anyInt())).thenAnswer(i -> {
            int t = i.getArgument(0, Integer.class);
            return s.model.newFixedSizeIntervalVar(s.ctx.start(t), 5, "m" + t);
        });
        return s;
    }

    private CompiledConstraint compiled(IEnumEnforcement.Types e, int gap) {
        Constraint c = ContribTestSupport.constraint("no_overlap", e, ConstraintScope.all());
        return ContribTestSupport.compiled(c, family, new NoOverlapParams("class-", gap));
    }

    @Test
    void identity() {
        assertEquals("no_overlap", contributor.type());
        assertTrue(contributor.support().hard() && contributor.support().soft() && contributor.support().relaxable());
    }

    @Test
    void hardSolveUsesOneGlobalNoOverlapOverTheMasterIntervals() {
        ContribTestSupport s = fixture();
        FakeEnforcer enforcer = s.hard();
        contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.STRICTLY_REQUIRED, 0), enforcer);
        List<ConstraintProto> noOverlap = s.constraints(ConstraintProto.ConstraintCase.NO_OVERLAP);
        assertEquals(1, noOverlap.size());
        assertEquals(3, noOverlap.get(0).getNoOverlap().getIntervalsCount());
        assertEquals(0, enforcer.instanceCalls, "the global constraint cannot be enforced: no instance literal is requested");
    }

    @Test
    void hardSolveWithAGapUsesInflatedOptionalIntervals() {
        ContribTestSupport s = fixture();
        contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.STRICTLY_REQUIRED, 3), s.hard());
        verify(s.ctx, never()).masterInterval(anyInt());
        List<ConstraintProto> noOverlap = s.constraints(ConstraintProto.ConstraintCase.NO_OVERLAP);
        assertEquals(1, noOverlap.size());
        assertEquals(3, noOverlap.get(0).getNoOverlap().getIntervalsCount());
        assertEquals(3, s.constraints(ConstraintProto.ConstraintCase.INTERVAL).size());
    }

    @Test
    void softUsesPairLiteralsOneInstancePerPair() {
        ContribTestSupport s = fixture();
        FakeEnforcer enforcer = s.soft();
        contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.SOFT, 0), enforcer);

        assertEquals(3, enforcer.instanceCalls); // pairs (0,1) (0,2) (1,2): soft unit = one overlapping pair
        assertEquals(0, s.constraints(ConstraintProto.ConstraintCase.NO_OVERLAP).size());
        List<ConstraintProto> clauses = s.constraints(ConstraintProto.ConstraintCase.BOOL_OR);
        assertEquals(3, clauses.size());
        for (int k = 0; k < 3; k++) {
            assertEquals(2, clauses.get(k).getBoolOr().getLiteralsCount()); // o1, o2 (both tasks required: no absence literals)
            assertEquals(List.of(enforcer.issued.get(k).getIndex()), clauses.get(k).getEnforcementLiteralList());
        }
        assertEquals(6, s.constraints(ConstraintProto.ConstraintCase.LINEAR).size()); // two ordering constraints per pair
    }

    @Test
    void relaxableAndExplainGuardEveryPairClauseWithTheSharedLiteral() {
        ContribTestSupport s = fixture();
        BoolVar active = s.model.newBoolVar("active");
        contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION, 0), s.relaxable(active));
        for (ConstraintProto clause : s.constraints(ConstraintProto.ConstraintCase.BOOL_OR)) {
            assertEquals(List.of(active.getIndex()), clause.getEnforcementLiteralList());
        }
        assertEquals(3, s.constraints(ConstraintProto.ConstraintCase.BOOL_OR).size());

        ContribTestSupport e = fixture();
        e.explain(true);
        BoolVar assumption = e.model.newBoolVar("assumption");
        contributor.apply(e.ctx, compiled(IEnumEnforcement.Types.STRICTLY_REQUIRED, 0), e.assumption(assumption));
        assertEquals(0, e.constraints(ConstraintProto.ConstraintCase.NO_OVERLAP).size());
        assertEquals(3, e.constraints(ConstraintProto.ConstraintCase.BOOL_OR).size());
        for (ConstraintProto clause : e.constraints(ConstraintProto.ConstraintCase.BOOL_OR)) {
            assertEquals(List.of(assumption.getIndex()), clause.getEnforcementLiteralList());
        }
    }

    @Test
    void optionalTasksAddTheirAbsenceToTheClause() {
        ContribTestSupport s = new ContribTestSupport(1);
        s.task(0, 50, true, 0, 5);
        s.task(0, 50, false, 0, 5);
        when(s.index.groupTasksByTagPrefix("class-")).thenReturn(new int[][]{{0, 1}});
        contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.SOFT, 0), s.soft());
        ConstraintProto clause = s.constraints(ConstraintProto.ConstraintCase.BOOL_OR).get(0);
        assertEquals(3, clause.getBoolOr().getLiteralsCount()); // o1, o2, not p0
    }

    @Test
    void pairsThatTheDomainsSeparateGetNoLiteral() {
        ContribTestSupport s = new ContribTestSupport(1);
        s.task(0, 0, false, 0, 5);   // ends at 5 at the latest
        s.task(20, 50, false, 0, 5); // starts at 20 at the earliest
        when(s.index.groupTasksByTagPrefix("class-")).thenReturn(new int[][]{{0, 1}});
        FakeEnforcer enforcer = s.soft();
        contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.SOFT, 0), enforcer);
        assertEquals(0, enforcer.instanceCalls);
        assertTrue(s.constraints().isEmpty());
    }

    @Test
    void tooManyPairsIsAnErrorInTheSolvePassAndSkippedInTheExplainPass() {
        ContribTestSupport s = fixture();
        s.maxPairwise(2);
        InvalidRequestException e = assertThrows(InvalidRequestException.class,
                () -> contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.SOFT, 0), s.soft()));
        assertEquals("PAIRWISE_TOO_LARGE", e.code());

        ContribTestSupport x = fixture();
        x.maxPairwise(2);
        x.explain(true);
        contributor.apply(x.ctx, compiled(IEnumEnforcement.Types.STRICTLY_REQUIRED, 0), x.assumption(x.model.newBoolVar("a")));
        assertTrue(x.constraints().isEmpty());
    }
}
