package eu.isygoit.solver.cpsat.contrib;

import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MultiResourceRequirementContributorTest {

    private final MultiResourceRequirementContributor contributor = new MultiResourceRequirementContributor();

    @Test
    void identityAndHardOnlySupport() {
        assertEquals("multi_resource_requirement", contributor.type());
        assertEquals(EnforcementSupport.HARD_ONLY, contributor.support());
    }

    @Test
    void applyDoesNotTouchTheModel() {
        // the design says: the requirement is a task rewrite, the structural model does the rest
        CpModelContext ctx = Mockito.mock(CpModelContext.class, invocation -> {
            throw new AssertionError("apply must not use the context: " + invocation.getMethod().getName());
        });
        contributor.apply(ctx, null, null);
    }
}
