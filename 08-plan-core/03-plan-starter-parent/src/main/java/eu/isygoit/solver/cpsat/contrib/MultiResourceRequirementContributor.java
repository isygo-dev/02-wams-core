package eu.isygoit.solver.cpsat.contrib;

import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatConstraintContributor;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.family.MultiResourceRequirementFamily;
import org.springframework.stereotype.Component;

/**
 * CP-SAT side of {@code multi_resource_requirement} (design 6.5.10): intentionally empty. The constraint is a task rewrite
 * ({@code MultiResourceRequirementFamily} as {@code TaskRewriter}, applied by {@code DomainNormalizer}) and the resulting
 * requirements are modelled by the structural model of 6.4. The bean exists so that the capability matrix reports the family
 * as supported by CP-SAT (HARD only). Stateless, thread-safe.
 */
@Component
public class MultiResourceRequirementContributor implements CpSatConstraintContributor {

    @Override
    public String type() {
        return MultiResourceRequirementFamily.TYPE;
    }

    @Override
    public EnforcementSupport support() {
        return EnforcementSupport.HARD_ONLY;
    }

    @Override
    public void apply(CpModelContext ctx, CompiledConstraint constraint, Enforcer enforcer) {
        // nothing to add: handled by the task rewrite (HARD only)
    }
}
