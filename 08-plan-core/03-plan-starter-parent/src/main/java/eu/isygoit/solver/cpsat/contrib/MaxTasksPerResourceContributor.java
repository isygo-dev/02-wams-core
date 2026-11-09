package eu.isygoit.solver.cpsat.contrib;

import eu.isygoit.dto.request.params.LoadMetric;
import eu.isygoit.dto.request.params.MaxTasksPerResourceParams;
import eu.isygoit.solver.CompiledConstraint;
import eu.isygoit.solver.cpsat.CpModelContext;
import eu.isygoit.solver.cpsat.CpSatConstraintContributor;
import eu.isygoit.solver.cpsat.EnforcementSupport;
import eu.isygoit.solver.cpsat.Enforcer;
import eu.isygoit.solver.family.MaxTasksPerResourceFamily;
import org.springframework.stereotype.Component;

/**
 * CP-SAT formulation of {@code max_tasks_per_resource} (design 6.5.15): the COUNT variant of the period load, over the whole
 * horizon ({@code period == null}, no bucket literals) or per period (start attribution). HARD, SOFT (excess / deficit in
 * tasks) and RELAXABLE through the {@link Enforcer}; shares {@code PeriodLoadEmitter} with
 * {@code max_load_per_period}. Stateless, thread-safe.
 */
@Component
public class MaxTasksPerResourceContributor implements CpSatConstraintContributor {

    @Override
    public String type() {
        return MaxTasksPerResourceFamily.TYPE;
    }

    @Override
    public EnforcementSupport support() {
        return EnforcementSupport.ALL;
    }

    @Override
    public void apply(CpModelContext ctx, CompiledConstraint constraint, Enforcer enforcer) {
        MaxTasksPerResourceParams p = (MaxTasksPerResourceParams) constraint.params();
        PeriodLoadEmitter.emit(ctx, constraint, enforcer, p.period(), LoadMetric.COUNT, p.max(), p.min());
    }
}
