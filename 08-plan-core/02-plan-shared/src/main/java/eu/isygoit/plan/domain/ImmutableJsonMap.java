package eu.isygoit.plan.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ImmutableJsonMap {
    private ImmutableJsonMap() {
    }

    public static Map<String, Object> copyOf(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copy = new LinkedHashMap<>(values);
        if (copy.containsKey(null)) {
            throw new IllegalArgumentException("JSON object keys must not be null");
        }
        return Collections.unmodifiableMap(copy);
    }
}
