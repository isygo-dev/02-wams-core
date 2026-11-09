package eu.isygoit.async.camel.processor;

import java.nio.charset.StandardCharsets;

/** Helpers to read the Kafka record key header (String or byte[] depending on the configured deserializer). */
final class PlanKafkaKeys {
    private PlanKafkaKeys() {
    }

    /** The record key as a string, or null when absent. */
    static String keyOf(Object key) {
        if (key == null) {
            return null;
        }
        return key instanceof byte[] bytes ? new String(bytes, StandardCharsets.UTF_8) : key.toString();
    }

    /** The given text parsed as a job id, or null when absent or not a number. */
    static Long jobIdOf(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(key.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
