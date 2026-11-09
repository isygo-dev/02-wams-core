package eu.isygoit.async.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.dto.tech.PlanJobMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PlanJobKafkaProducerTest {
    private final PlanKafkaSender sender = mock(PlanKafkaSender.class);
    private final PlanJobKafkaProducer producer = new PlanJobKafkaProducer(sender);

    PlanJobKafkaProducerTest() {
        producer.setTopic("plan.jobs");
        ReflectionTestUtils.setField(producer, "sendTimeoutMs", 200L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendsTheJobIdAsKeyAndASmallJsonBodyToTheJobsTopic() throws Exception {
        when(sender.send(eq("plan.jobs"), anyString(), any(byte[].class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        producer.dispatch(new PlanJobMessage(123L, "acme"));

        ArgumentCaptor<byte[]> body = ArgumentCaptor.forClass(byte[].class);
        verify(sender).send(eq("plan.jobs"), eq("123"), body.capture());
        PlanJobMessage sent = new ObjectMapper().readValue(body.getValue(), PlanJobMessage.class);
        assertEquals(new PlanJobMessage(123L, "acme"), sent);
    }

    @Test
    void brokerFailureSurfacesAsKafkaException() {
        when(sender.send(anyString(), anyString(), any(byte[].class)))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker down")));

        assertThrows(KafkaException.class, () -> producer.dispatch(new PlanJobMessage(1L, "acme")));
    }

    @Test
    void timeoutSurfacesAsKafkaException() {
        when(sender.send(anyString(), anyString(), any(byte[].class))).thenReturn(new CompletableFuture<>());

        assertThrows(KafkaException.class, () -> producer.dispatch(new PlanJobMessage(1L, "acme")));
    }

    @Test
    void enqueueFailureOfTheSenderIsPropagated() {
        when(sender.send(anyString(), anyString(), any(byte[].class))).thenThrow(new KafkaException("block timeout"));

        assertThrows(KafkaException.class, () -> producer.dispatch(new PlanJobMessage(1L, "acme")));
    }

    @Test
    void missingMessageOrJobIdIsRejectedWithoutSending() {
        assertThrows(IllegalArgumentException.class, () -> producer.dispatch(null));
        assertThrows(IllegalArgumentException.class, () -> producer.dispatch(new PlanJobMessage(null, "acme")));
        verifyNoInteractions(sender);
    }

    @Test
    void unconfiguredTopicIsRejected() {
        PlanJobKafkaProducer unconfigured = new PlanJobKafkaProducer(sender);

        assertThrows(IllegalStateException.class, () -> unconfigured.dispatch(new PlanJobMessage(1L, "acme")));
        verifyNoInteractions(sender);
    }
}
