package eu.isygoit.async.camel.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.dto.tech.PlanJobMessage;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanKafkaErrorsTest {
    @Test
    void unparseableJsonIsNonRetryable() {
        JsonProcessingException parseError = assertThrows(JsonProcessingException.class,
                () -> new ObjectMapper().readValue("{not json", PlanJobMessage.class));

        assertTrue(PlanKafkaErrors.isNonRetryable(parseError));
        assertTrue(PlanKafkaErrors.isNonRetryable(new RuntimeException("wrapped", parseError)));
    }

    @Test
    void poisonMessageIsNonRetryable() {
        assertTrue(PlanKafkaErrors.isNonRetryable(new PoisonMessageException("empty")));
    }

    @Test
    void transientFailuresAreRetryable() {
        assertFalse(PlanKafkaErrors.isNonRetryable(new IllegalStateException("db down")));
        assertFalse(PlanKafkaErrors.isNonRetryable(new IOException("io")));
        assertFalse(PlanKafkaErrors.isNonRetryable(null));
    }
}
