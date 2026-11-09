package eu.isygoit.solver.cpsat;

import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.Literal;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The assumption registry of the EXPLAIN pass (design 6.10): caching by (kind, id, reason), creation order, translation of
 * a solver core back into items. Only builds literals on a {@link CpModel} (no solve); skipped when the OR-Tools model
 * classes cannot be loaded.
 */
class AssumptionRegistryTest {

    private CpModel model;
    private AssumptionRegistry registry;

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
        registry = new AssumptionRegistry(model);
    }

    @Test
    void sameItemReturnsTheSameLiteralAndDifferentReasonsDiffer() {
        Literal a = registry.literal(AssumptionRegistry.KIND_TASK, "t1", "WINDOW_EMPTY");
        Literal again = registry.literal(AssumptionRegistry.KIND_TASK, "t1", "WINDOW_EMPTY");
        Literal pin = registry.literal(AssumptionRegistry.KIND_TASK, "t1", "PIN_CONFLICT");
        assertSame(a, again);
        assertNotSame(a, pin);
        assertEquals(2, registry.size());
    }

    @Test
    void refsKeepTheCreationOrderAndMetadata() {
        registry.literal(AssumptionRegistry.KIND_CONSTRAINT, "rest", "CONSTRAINT");
        registry.literal(AssumptionRegistry.KIND_RESOURCE, "ana", "CALENDAR");
        registry.literal(AssumptionRegistry.KIND_EDGE, "a->b", "PRECEDENCE");
        List<AssumptionRef> refs = registry.refs();
        assertEquals(3, refs.size());
        assertEquals("CONSTRAINT", refs.get(0).kind());
        assertEquals("rest", refs.get(0).id());
        assertEquals("CALENDAR", refs.get(1).reasonCode());
        assertEquals("a->b", refs.get(2).id());
        assertEquals(3, registry.literals().length);
        assertSame(refs.get(1).literal(), registry.literals()[1]);
    }

    @Test
    void resolveTranslatesACoreInSolverOrderWithoutDuplicates() {
        Literal first = registry.literal(AssumptionRegistry.KIND_TASK, "t1", "PIN_CONFLICT");
        Literal second = registry.literal(AssumptionRegistry.KIND_CONSTRAINT, "cap", "CONSTRAINT");
        registry.literal(AssumptionRegistry.KIND_EDGE, "x->y", "PRECEDENCE");
        List<AssumptionRef> blamed = registry.resolve(List.of(second.getIndex(), first.getIndex(), second.getIndex(), 987654));
        assertEquals(2, blamed.size());
        assertEquals("cap", blamed.get(0).id());
        assertEquals("t1", blamed.get(1).id());
        assertTrue(registry.resolve(null).isEmpty());
    }

    @Test
    void lookupByLiteralIndexFindsOnlyAssumptions() {
        Literal assumption = registry.literal(AssumptionRegistry.KIND_RESOURCE, "r", "CAPACITY_EXCEEDED");
        Literal other = model.newBoolVar("other");
        assertEquals("r", registry.byLiteralIndex(assumption.getIndex()).id());
        assertNull(registry.byLiteralIndex(other.getIndex()));
    }

    @Test
    void installReplacesTheAssumptionsOfTheModel() {
        registry.literal(AssumptionRegistry.KIND_CONSTRAINT, "c1", "CONSTRAINT");
        registry.literal(AssumptionRegistry.KIND_CONSTRAINT, "c2", "CONSTRAINT");
        registry.install();
        registry.install(registry.refs().subList(0, 1));
        registry.install(List.of());
        // the proto keeps exactly the last installed set (empty): no assumptions are left on the model
        assertEquals(0, model.getBuilder().getAssumptionsCount());
        registry.install();
        assertEquals(2, model.getBuilder().getAssumptionsCount());
    }
}
