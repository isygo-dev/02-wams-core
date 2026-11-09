package eu.isygoit.util;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Null-safe defensive copies used by the compact constructors of the immutable IR records.
 * <p>
 * Every method maps {@code null} to an empty immutable collection and otherwise returns an unmodifiable copy that
 * PRESERVES the iteration order of the input (request order is what makes model building and JSON output
 * deterministic: no hash ordering).
 */
public final class CollectionCopies {

    private CollectionCopies() {
    }

    /**
     * Immutable copy of a list; {@code null} gives an empty list. Null elements are rejected.
     */
    public static <T> List<T> list(Collection<? extends T> values) {
        return values == null || values.isEmpty() ? List.of() : List.copyOf(values);
    }

    /**
     * Unmodifiable insertion-ordered copy of a set; {@code null} gives an empty set. Null elements are rejected.
     */
    public static <T> Set<T> orderedSet(Collection<? extends T> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        Set<T> copy = new LinkedHashSet<>(values.size() * 2);
        for (T value : values) {
            copy.add(Objects.requireNonNull(value, "collection elements must not be null"));
        }
        return Collections.unmodifiableSet(copy);
    }

    /**
     * Unmodifiable copy of a set of enum constants in declaration order; {@code null} gives an empty set.
     */
    public static <E extends Enum<E>> Set<E> enumSet(Collection<E> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(values));
    }

    /**
     * Unmodifiable insertion-ordered copy of a map; {@code null} gives an empty map. Null keys are rejected.
     */
    public static <K, V> Map<K, V> orderedMap(Map<K, V> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<K, V> copy = new LinkedHashMap<>(values.size() * 2);
        for (Map.Entry<K, V> entry : values.entrySet()) {
            copy.put(Objects.requireNonNull(entry.getKey(), "map keys must not be null"), entry.getValue());
        }
        return Collections.unmodifiableMap(copy);
    }
}
