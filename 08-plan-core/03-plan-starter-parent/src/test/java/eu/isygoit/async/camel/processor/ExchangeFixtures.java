package eu.isygoit.async.camel.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.component.kafka.consumer.KafkaManualCommit;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Mock-based Camel exchange for the Kafka processor tests (no CamelContext needed). */
final class ExchangeFixtures {
    final Exchange exchange = mock(Exchange.class);
    final Message message = mock(Message.class);
    final KafkaManualCommit manualCommit = mock(KafkaManualCommit.class);

    ExchangeFixtures(String body, String key, Throwable caught) {
        when(exchange.getIn()).thenReturn(message);
        when(message.getBody(String.class)).thenReturn(body);
        when(message.getBody(byte[].class)).thenReturn(body == null ? null : body.getBytes(StandardCharsets.UTF_8));
        when(message.getHeader(KafkaConstants.MANUAL_COMMIT, KafkaManualCommit.class)).thenReturn(manualCommit);
        when(message.getHeader(KafkaConstants.KEY)).thenReturn(key);
        when(message.getHeader(KafkaConstants.TOPIC)).thenReturn("source-topic");
        when(message.getHeader(KafkaConstants.PARTITION)).thenReturn(0);
        when(message.getHeader(KafkaConstants.OFFSET)).thenReturn(42L);
        when(message.getHeader(eq(Exchange.REDELIVERY_COUNTER), eq(Integer.class))).thenReturn(3);
        when(exchange.getProperty(eq(Exchange.EXCEPTION_CAUGHT), eq(Throwable.class))).thenReturn(caught);
    }

    ExchangeFixtures withoutCommitHandle() {
        when(message.getHeader(KafkaConstants.MANUAL_COMMIT, KafkaManualCommit.class)).thenReturn(null);
        return this;
    }
}
