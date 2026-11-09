package eu.isygoit.solver.registry;

import eu.isygoit.exception.UnknownObjectiveException;
import eu.isygoit.solver.spi.ObjectiveFamily;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of all {@link ObjectiveFamily} beans, keyed by canonical (trimmed, lower-case) type (design 5.7).
 * <p>
 * Created in {@code PlanningEngineConfiguration} (WP7); tests construct it by hand. Immutable after construction and
 * thread-safe; iteration order is ascending type. Fails FAST at construction on a blank or duplicate type with an
 * {@link IllegalStateException} naming both classes.
 */
public final class ObjectiveFamilyRegistry {

    private final Map<String, ObjectiveFamily> families;

    /**
     * @param families all objective family beans
     * @throws IllegalStateException on duplicate (or blank) type
     */
    public ObjectiveFamilyRegistry(Collection<ObjectiveFamily> families) {
        this.families = TypeIndex.index(families, ObjectiveFamily::type, "objective family");
    }

    /**
     * @param type the objective type (case-insensitive)
     * @return the family
     * @throws UnknownObjectiveException when the type is {@code null} or not registered
     */
    public ObjectiveFamily get(String type) {
        ObjectiveFamily family = families.get(TypeIndex.canonical(type));
        if (family == null) {
            throw new UnknownObjectiveException(type, families.keySet());
        }
        return family;
    }

    /** @return the family, or empty when the type is {@code null} or not registered */
    public Optional<ObjectiveFamily> find(String type) {
        return Optional.ofNullable(families.get(TypeIndex.canonical(type)));
    }

    /** @return {@code true} when the type is registered */
    public boolean contains(String type) {
        return families.containsKey(TypeIndex.canonical(type));
    }

    /** @return the canonical registered types, ascending, unmodifiable */
    public Collection<String> supportedTypes() {
        return families.keySet();
    }

    /** @return every family, ordered by type, unmodifiable */
    public Collection<ObjectiveFamily> families() {
        return families.values();
    }
}
