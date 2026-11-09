package eu.isygoit.solver.registry;

import eu.isygoit.exception.UnknownConstraintException;
import eu.isygoit.solver.spi.ConstraintFamily;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of all {@link ConstraintFamily} beans, keyed by canonical (trimmed, lower-case) type (design 5.7).
 * <p>
 * Created in {@code PlanningEngineConfiguration} (WP7) from the collection of family beans (constructor injection);
 * tests construct it by hand with {@code List.of(...)}. Immutable after construction and thread-safe; iteration order is
 * ascending type. Fails FAST at construction on a blank or duplicate type with an {@link IllegalStateException} naming both
 * classes.
 */
public final class ConstraintFamilyRegistry {

    private final Map<String, ConstraintFamily> families;

    /**
     * @param families all family beans
     * @throws IllegalStateException on duplicate (or blank) type
     */
    public ConstraintFamilyRegistry(Collection<ConstraintFamily> families) {
        this.families = TypeIndex.index(families, ConstraintFamily::type, "constraint family");
    }

    /**
     * @param type the constraint type (case-insensitive)
     * @return the family
     * @throws UnknownConstraintException when the type is {@code null} or not registered
     */
    public ConstraintFamily get(String type) {
        ConstraintFamily family = families.get(TypeIndex.canonical(type));
        if (family == null) {
            throw new UnknownConstraintException(type, families.keySet());
        }
        return family;
    }

    /** @return the family, or empty when the type is {@code null} or not registered */
    public Optional<ConstraintFamily> find(String type) {
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
    public Collection<ConstraintFamily> families() {
        return families.values();
    }
}
