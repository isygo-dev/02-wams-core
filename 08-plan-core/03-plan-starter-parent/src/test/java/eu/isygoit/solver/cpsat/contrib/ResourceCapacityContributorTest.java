package eu.isygoit.solver.cpsat.contrib;

import com.google.ortools.sat.BoolVar;
import eu.isygoit.dto.request.Constraint;
import eu.isygoit.dto.request.ConstraintScope;
import eu.isygoit.dto.request.TimeWindow;
import eu.isygoit.dto.request.params.ResourceCapacityParams;
import eu.isygoit.enums.IEnumEnforcement;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.family.ResourceCapacityFamily;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Verifies the blocks registered with the stub context; the Cumulative / NoOverlap creation itself belongs to WP3. */
class ResourceCapacityContributorTest {

    private final ResourceCapacityContributor contributor = new ResourceCapacityContributor();
    private final ResourceCapacityFamily family = new ResourceCapacityFamily();

    /** r0 unary, r1 a pool of capacity 3, both in scope. */
    private ContribTestSupport fixture() {
        ContribTestSupport s = new ContribTestSupport(2);
        when(s.index.resolveResources(any())).thenReturn(new int[]{0, 1});
        when(s.index.capacity(0)).thenReturn(1);
        when(s.index.capacity(1)).thenReturn(3);
        return s;
    }

    private CompiledConstraint compiled(IEnumEnforcement.Types e, ResourceCapacityParams p) {
        Constraint c = ContribTestSupport.constraint("resource_capacity", e, ConstraintScope.all());
        return ContribTestSupport.compiled(c, family, p);
    }

    private static TimeWindow window(int from, int to) {
        return new TimeWindow(ContribTestSupport.T0.plusSeconds(from * 60L), ContribTestSupport.T0.plusSeconds(to * 60L));
    }

    @Test
    void supportsHardAndRelaxableButNotSoft() {
        assertTrue(contributor.support().hard());
        assertTrue(contributor.support().relaxable());
        assertFalse(contributor.support().soft());
        assertTrue(contributor.type().equals("resource_capacity"));
    }

    @Test
    void hardBlocksTheLoweredUnitsUnconditionallyInsideTheWindow() {
        ContribTestSupport s = fixture();
        contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.STRICTLY_REQUIRED, new ResourceCapacityParams(1, window(10, 30))), s.hard());
        verify(s.ctx, never()).blockResource(eq(0), anyInt(), anyInt(), anyInt(), any()); // the unary resource is not lowered by capacity 1
        verify(s.ctx).blockResource(eq(1), eq(10), eq(30), eq(2), isNull()); // 3 -> 1: two units blocked, no enforcement
    }

    @Test
    void aUnaryResourceReducedToZeroIsBlockedCompletelyOverTheHorizonByDefault() {
        ContribTestSupport s = fixture();
        contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.STRICTLY_REQUIRED, new ResourceCapacityParams(0, null)), s.hard());
        verify(s.ctx).blockResource(eq(0), eq(0), eq(100), eq(1), isNull());
        verify(s.ctx).blockResource(eq(1), eq(0), eq(100), eq(3), isNull());
    }

    @Test
    void relaxableGuardsTheBlockWithTheSharedActiveLiteral() {
        ContribTestSupport s = fixture();
        BoolVar active = s.model.newBoolVar("active");
        contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.RELAXABLE_WITH_AUTHORIZATION, new ResourceCapacityParams(2, null)),
                s.relaxable(active));
        verify(s.ctx).blockResource(eq(1), eq(0), eq(100), eq(1), same(active));
    }

    @Test
    void explainPassGuardsTheBlockWithTheAssumptionLiteral() {
        ContribTestSupport s = fixture();
        s.explain(true);
        BoolVar assumption = s.model.newBoolVar("assumption");
        contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.STRICTLY_REQUIRED, new ResourceCapacityParams(2, null)), s.assumption(assumption));
        verify(s.ctx).blockResource(eq(1), eq(0), eq(100), eq(1), same(assumption));
    }

    @Test
    void aWindowOutsideTheHorizonBlocksNothing() {
        ContribTestSupport s = fixture();
        contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.STRICTLY_REQUIRED, new ResourceCapacityParams(0, window(200, 300))), s.hard());
        verify(s.ctx, never()).blockResource(anyInt(), anyInt(), anyInt(), anyInt(), any());
    }

    @Test
    void softEnforcementIsRejected() {
        ContribTestSupport s = fixture();
        assertThrows(IllegalStateException.class,
                () -> contributor.apply(s.ctx, compiled(IEnumEnforcement.Types.SOFT, new ResourceCapacityParams(1, null)), s.soft()));
    }
}
