package eu.isygoit.async.camel.processor;

import eu.isygoit.async.kafka.PlanDeadLetterProducer;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.kafka.KafkaException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PlanLogDeadLetterProcessorTest {
    private final PlanDeadLetterProducer deadLetterProducer = mock(PlanDeadLetterProducer.class);
    private final PlanLogDeadLetterProcessor processor = new PlanLogDeadLetterProcessor(deadLetterProducer, "logs.dlq");

    @Test
    void publishesToTheLogDeadLetterTopicThenCommits() {
        ExchangeFixtures fixtures = new ExchangeFixtures("{bad", null, new RuntimeException("cassandra down"));

        processor.process(fixtures.exchange);

        InOrder order = inOrder(deadLetterProducer, fixtures.manualCommit);
        order.verify(deadLetterProducer).publish(eq("logs.dlq"), eq(null), any(byte[].class), any());
        order.verify(fixtures.manualCommit).commit();
    }

    @Test
    void logsAreBestEffortSoTheOffsetIsCommittedEvenWhenTheDeadLetterPublishFails() {
        doThrow(new KafkaException("broker down")).when(deadLetterProducer)
                .publish(anyString(), any(), any(), any());
        ExchangeFixtures fixtures = new ExchangeFixtures("{bad", null, new RuntimeException("x"));

        processor.process(fixtures.exchange);

        verify(fixtures.manualCommit).commit();
    }
}
