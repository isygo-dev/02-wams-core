package eu.isygoit.solver.registry;

import eu.isygoit.solver.cpsat.CpSatConstraintContributor;
import eu.isygoit.solver.cpsat.CpSatObjectiveContributor;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of the CP-SAT contributors (constraints and objectives), keyed by canonical (trimmed, lower-case) type
 * (design 5.7). Source of {@code CpSatSolverEngine.capabilities()}.
 * <p>
 * Created in {@code PlanningEngineConfiguration} (WP7); tests construct it by hand. Immutable after construction and
 * thread-safe; iteration order is ascending type. Fails FAST at construction on a blank or duplicate type (per
 * contributor kind) with an {@link IllegalStateException} naming both classes. Imports no OR-Tools class beyond the
 * contributor interfaces (whose signatures reference them).
 */
public final class CpSatContributorRegistry {

    private final Map<String, CpSatConstraintContributor> constraints;
    private final Map<String, CpSatObjectiveContributor> objectives;

    /**
     * @param constraints all constraint contributor beans
     * @param objectives  all objective contributor beans
     * @throws IllegalStateException on duplicate (or blank) type
     */
    public CpSatContributorRegistry(
            Collection<CpSatConstraintContributor> constraints, Collection<CpSatObjectiveContributor> objectives) {
        this.constraints = TypeIndex.index(constraints, CpSatConstraintContributor::type, "CP-SAT constraint contributor");
        this.objectives = TypeIndex.index(objectives, CpSatObjectiveContributor::type, "CP-SAT objective contributor");
    }

    /** @return the contributor of the constraint type, or empty when none (or the type is {@code null}) */
    public Optional<CpSatConstraintContributor> constraint(String type) {
        return Optional.ofNullable(constraints.get(TypeIndex.canonical(type)));
    }

    /** @return the contributor of the objective type, or empty when none (or the type is {@code null}) */
    public Optional<CpSatObjectiveContributor> objective(String type) {
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
