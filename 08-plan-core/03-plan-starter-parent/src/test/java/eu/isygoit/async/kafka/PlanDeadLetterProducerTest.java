package eu.isygoit.async.kafka;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.support.SendResult;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PlanDeadLetterProducerTest {
    private final PlanKafkaSender sender = mock(PlanKafkaSender.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final PlanDeadLetterProducer producer =
            new PlanDeadLetterProducer(sender, new PlanKafkaMetrics(registry), 200);

    @Test
    @SuppressWarnings("unchecked")
    void sendsOriginalPayloadAndHeadersAndCountsTheDeadLetter() {
        byte[] payload = "{bad".getBytes();
        Map<String, String> headers = Map.of("x-attempts", "4");
        when(sender.send("dlq", "key", payload, headers))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        producer.publish("dlq", "key", payload, headers);

        verify(sender).send("dlq", "key", payload, headers);
        assertEquals(1.0, registry.counter("plan.kafka.dlq", "topic", "dlq").count());
    }

    @Test
    void brokerFailureSurfacesAsKafkaExceptionWithoutCounting() {
        when(sender.send(eq("dlq"), any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker down")));

        assertThrows(KafkaException.class, () -> producer.publish("dlq", null, new byte[0], Map.of()));
        assertEquals(0.0, registry.counter("plan.kafka.dlq", "topic", "dlq").count());
    }

    @Test
    void timeoutSurfacesAsKafkaException() {
        when(sender.send(eq("dlq"), any(), any(), any())).thenReturn(new CompletableFuture<>());

        assertThrows(KafkaException.class, () -> producer.publish("dlq", null, new byte[0], Map.of()));
    }

    @Test
    void blankTopicIsRejected() {
        assertThrows(IllegalStateException.class, () -> producer.publish(" ", null, new byte[0], Map.of()));
        verifyNoInteractions(sender);
    }
}
