package eu.isygoit.solver.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Shared index builder of the registries: canonical type ids, duplicate detection and a deterministic, immutable
 * map sorted by canonical type.
 * <p>
 * Case policy: type ids are canonicalised with {@link #canonical(String)} (trimmed, lower-case, {@link Locale#ROOT}) both at
 * registration and at lookup, so {@code "Min_Rest"} and {@code "min_rest"} are the same type (and a duplicate).
 * Package-private helper; stateless and thread-safe.
 */
final class TypeIndex {

    private TypeIndex() {
    }

    /** @return the canonical form of a type id, or {@code null} for {@code null} */
    static String canonical(String type) {
        return type == null ? null : type.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Builds an unmodifiable map keyed by canonical type, iterating in ascending type order.
     *
     * @param beans  the beans (a {@code null} collection is treated as empty)
     * @param typeOf extracts the declared type id of a bean
     * @param kind   human label used in failure messages (for example {@code "constraint family"})
     * @throws IllegalStateException on a blank type or a duplicate type; the message names both classes
     */
    static <T> Map<String, T> index(Collection<? extends T> beans, Function<T, String> typeOf, String kind) {
        Map<String, T> result = new TreeMap<>();
        if (beans != null) {
            for (T bean : beans) {
                if (bean == null) {
                    throw new IllegalStateException("Null " + kind + " bean in registry");
                }
                String type = canonical(typeOf.apply(bean));
                if (type == null || type.isEmpty()) {
                    throw new IllegalStateException(
                            "The " + kind + " " + bean.getClass().getName() + " declares a blank type()");
                }
                T previous = result.putIfAbsent(type, bean);
                if (previous != null) {
                    throw new IllegalStateException("Duplicate " + kind + " type '" + type + "': registered by "
                            + previous.getClass().getName() + " and " + bean.getClass().getName());
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }
}
