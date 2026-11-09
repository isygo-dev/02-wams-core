package eu.isygoit.solver.cpsat.contrib;

import eu.isygoit.dto.request.params.MaxLoadPerPeriodParams;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatConstraintContributor;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.family.MaxLoadPerPeriodFamily;
import org.springframework.stereotype.Component;

/**
 * CP-SAT formulation of {@code max_load_per_period} (design 6.5.9). A task counts in the period of its start
 * ({@code startBucket} literals); the per-(resource, period) load is bounded above by {@code max} and below by {@code min}.
 * HARD, SOFT (excess / deficit measure in ticks or tasks) and RELAXABLE are implemented through the {@link Enforcer};
 * all work is done by {@code PeriodLoadEmitter}. Stateless, thread-safe; ticks.
 */
@Component
public class MaxLoadPerPeriodContributor implements CpSatConstraintContributor {

    @Override
    public String type() {
        return MaxLoadPerPeriodFamily.TYPE;
    }

    @Override
    public EnforcementSupport support() {
        return EnforcementSupport.ALL;
    }

    @Override
    public void apply(CpModelContext ctx, CompiledConstraint constraint, Enforcer enforcer) {
        MaxLoadPerPeriodParams p = (MaxLoadPerPeriodParams) constraint.params();
        PeriodLoadEmitter.emit(ctx, constraint, enforcer, p.period(), p.metric(), p.max(), p.min());
    }
}
