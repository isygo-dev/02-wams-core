package eu.isygoit.solver.registry;

import eu.isygoit.solver.timefold.TimefoldConstraintContributor;
import eu.isygoit.solver.timefold.TimefoldObjectiveContributor;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of the Timefold contributors (constraints and objectives), keyed by canonical (trimmed, lower-case) type
 * (design 5.7). Source of {@code TimefoldSolverEngine.capabilities()}.
 * <p>
 * Created in {@code PlanningEngineConfiguration} (WP7); tests construct it by hand. Immutable after construction and
 * thread-safe; iteration order is ascending type. Fails FAST at construction on a blank or duplicate type (per
 * contributor kind) with an {@link IllegalStateException} naming both classes.
 */
public final class TimefoldContributorRegistry {

    private final Map<String, TimefoldConstraintContributor> constraints;
    private final Map<String, TimefoldObjectiveContributor> objectives;

    /**
     * @param constraints all constraint contributor beans
     * @param objectives  all objective contributor beans
     * @throws IllegalStateException on duplicate (or blank) type
     */
    public TimefoldContributorRegistry(
            Collection<TimefoldConstraintContributor> constraints, Collection<TimefoldObjectiveContributor> objectives) {
        this.constraints = TypeIndex.index(constraints, TimefoldConstraintContributor::type, "Timefold constraint contributor");
        this.objectives = TypeIndex.index(objectives, TimefoldObjectiveContributor::type, "Timefold objective contributor");
    }

    /** @return the contributor of the constraint type, or empty when none (or the type is {@code null}) */
    public Optional<TimefoldConstraintContributor> constraint(String type) {
        return Optional.ofNullable(constraints.get(TypeIndex.canonical(type)));
    }

    /** @return the contributor of the objective type, or empty when none (or the type is {@code null}) */
    public Optional<TimefoldObjectiveContributor> objective(String type) {
        return Optional.ofNullable(objectives.get(TypeIndex.canonical(type)));
    }

    /** @return the constraint types that have a contributor, ascending, unmodifiable */
    public Collection<String> constraintTypes() {
        return constraints.keySet();
    }

    /** @return the objective types that have a contributor, ascending, unmodifiable */
    public Collection<String> objectiveTypes() {
        return objectives.keySet();
    }
}
