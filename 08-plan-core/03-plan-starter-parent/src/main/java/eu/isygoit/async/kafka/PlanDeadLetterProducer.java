package eu.isygoit.async.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.KafkaException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publishes a poison / exhausted message to a dead-letter topic through {@link PlanKafkaSender} (byte[]/byte[]),
 * keeping the original payload and key untouched. Blocks until the broker acknowledges (bounded by
 * app.plan.kafka.send-timeout-ms) because the caller commits the source offset only after this returns; failures are
 * reported as {@link KafkaException} so the offset stays uncommitted.
 */
@Component
public class PlanDeadLetterProducer {
    public static final String HEADER_EXCEPTION_CLASS = "x-exception-class";
    public static final String HEADER_EXCEPTION_MESSAGE = "x-exception-message";
    public static final String HEADER_ORIGINAL_TOPIC = "x-original-topic";
    public static final String HEADER_ORIGINAL_PARTITION = "x-original-partition";
    public static final String HEADER_ORIGINAL_OFFSET = "x-original-offset";
    public static final String HEADER_ATTEMPTS = "x-attempts";

    private final PlanKafkaSender sender;
    private final PlanKafkaMetrics metrics;
    private final long sendTimeoutMs;

    public PlanDeadLetterProducer(
            PlanKafkaSender sender,
            PlanKafkaMetrics metrics,
            @Value("${app.plan.kafka.send-timeout-ms:15000}") long sendTimeoutMs) {
        this.sender = sender;
        this.metrics = metrics;
        this.sendTimeoutMs = sendTimeoutMs;
    }

    public void publish(String dlqTopic, String key, byte[] payload, Map<String, String> headers) {
        if (dlqTopic == null || dlqTopic.isBlank()) {
            throw new IllegalStateException("Dead-letter Kafka topic is not configured");
        }
        try {
            sender.send(dlqTopic, key, payload == null ? new byte[0] : payload,
                    headers == null ? new LinkedHashMap<>() : headers)
                    .get(sendTimeoutMs, TimeUnit.MILLISECONDS);
            metrics.deadLettered(dlqTopic);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new KafkaException("Interrupted while publishing to dead-letter topic " + dlqTopic, exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new KafkaException("Could not publish to dead-letter topic " + dlqTopic, exception);
        }
    }
}
