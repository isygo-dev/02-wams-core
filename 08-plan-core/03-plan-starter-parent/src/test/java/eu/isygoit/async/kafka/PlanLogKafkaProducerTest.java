package eu.isygoit.async.kafka;

import eu.isygoit.dto.data.LogEntry;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PlanLogKafkaProducerTest {
    private final PlanKafkaSender sender = mock(PlanKafkaSender.class);
    private final PlanLogKafkaProducer producer = new PlanLogKafkaProducer(sender);

    PlanLogKafkaProducerTest() {
        producer.setTopic("plan.logs");
    }

    private LogEntry entry() {
        return new LogEntry(1L, "acme", Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:01Z"),
                UUID.randomUUID(), null, "step", "engine", "message", null, 0L, null, null);
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishSendsToTheLogsTopic() {
        when(sender.send(eq("plan.logs"), eq(null), any(byte[].class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        producer.publish(entry());

        verify(sender).send(eq("plan.logs"), eq(null), any(byte[].class));
    }

    @Test
    void publishNeverThrowsWhenTheSenderFailsOrTheFutureFails() {
        when(sender.send(eq("plan.logs"), eq(null), any(byte[].class)))
                .thenThrow(new KafkaException("block timeout"))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker down")));

        assertDoesNotThrow(() -> producer.publish(entry()));
        assertDoesNotThrow(() -> producer.publish(entry()));
    }

    @Test
    void publishNeverThrowsWhenTheTopicIsNotConfiguredAndIgnoresNull() {
        PlanLogKafkaProducer unconfigured = new PlanLogKafkaProducer(sender);

        assertDoesNotThrow(() -> unconfigured.publish(entry()));
        assertDoesNotThrow(() -> unconfigured.publish(null));
        verifyNoInteractions(sender);
    }
}
