package eu.isygoit.async.camel.processor;

import eu.isygoit.async.kafka.PlanDeadLetterProducer;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.kafka.KafkaConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Terminal step of a failed plan consumer exchange (invoked from the onException blocks of the routes): publishes the
 * original payload with failure metadata to the dead-letter topic and then commits the source offset so the partition
 * keeps flowing. If the dead-letter publish fails the exception propagates, the offset stays uncommitted and
 * breakOnFirstError makes the consumer re-poll the record.
 */
public abstract class AbstractPlanDeadLetterProcessor implements Processor {
    private static final int MAX_MESSAGE_LENGTH = 1000;

    protected final Logger log = LoggerFactory.getLogger(getClass());
    private final PlanDeadLetterProducer deadLetterProducer;
    private final String deadLetterTopic;
    private final boolean commitWhenDeadLetterFails;

    protected AbstractPlanDeadLetterProcessor(
            PlanDeadLetterProducer deadLetterProducer, String deadLetterTopic, boolean commitWhenDeadLetterFails) {
        this.deadLetterProducer = deadLetterProducer;
        this.deadLetterTopic = deadLetterTopic;
        this.commitWhenDeadLetterFails = commitWhenDeadLetterFails;
    }

    @Override
    public void process(Exchange exchange) {
        Throwable failure = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
        byte[] payload = exchange.getIn().getBody(byte[].class);
        String key = PlanKafkaKeys.keyOf(exchange.getIn().getHeader(KafkaConstants.KEY));

        try {
            deadLetterProducer.publish(deadLetterTopic, key, payload, headersOf(exchange, failure));
        } catch (RuntimeException exception) {
            if (!commitWhenDeadLetterFails) {
                throw exception;
            }
            log.error("Dropping message from {} partition {} offset {}: dead-letter publish failed: {}",
                    exchange.getIn().getHeader(KafkaConstants.TOPIC),
                    exchange.getIn().getHeader(KafkaConstants.PARTITION),
                    exchange.getIn().getHeader(KafkaConstants.OFFSET), exception.getMessage());
        }

        try {
            afterDeadLetter(payload, key, failure);
        } catch (RuntimeException exception) {
            log.error("Post dead-letter handling failed: {}", exception.getMessage(), exception);
        }

        PlanKafkaCommit.commit(exchange);
    }

    /** Hook run after the dead-letter publish and before the commit; failures are logged and swallowed. */
    protected void afterDeadLetter(byte[] payload, String key, Throwable failure) {
    }

    private Map<String, String> headersOf(Exchange exchange, Throwable failure) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (failure != null) {
            headers.put(PlanDeadLetterProducer.HEADER_EXCEPTION_CLASS, failure.getClass().getName());
            headers.put(PlanDeadLetterProducer.HEADER_EXCEPTION_MESSAGE, truncate(failure.getMessage()));
        }
        putIfPresent(headers, PlanDeadLetterProducer.HEADER_ORIGINAL_TOPIC,
                exchange.getIn().getHeader(KafkaConstants.TOPIC));
        putIfPresent(headers, PlanDeadLetterProducer.HEADER_ORIGINAL_PARTITION,
                exchange.getIn().getHeader(KafkaConstants.PARTITION));
        putIfPresent(headers, PlanDeadLetterProducer.HEADER_ORIGINAL_OFFSET,
                exchange.getIn().getHeader(KafkaConstants.OFFSET));
        Integer redeliveries = exchange.getIn().getHeader(Exchange.REDELIVERY_COUNTER, Integer.class);
        headers.put(PlanDeadLetterProducer.HEADER_ATTEMPTS,
                String.valueOf((redeliveries == null ? 0 : redeliveries) + 1));
        return headers;
    }

    private static void putIfPresent(Map<String, String> headers, String name, Object value) {
        if (value != null) {
            headers.put(name, String.valueOf(value));
        }
    }

    private static String truncate(String message) {
        if (message == null) {
            return "";
        }
        return message.length() <= MAX_MESSAGE_LENGTH ? message : message.substring(0, MAX_MESSAGE_LENGTH);
    }
}
