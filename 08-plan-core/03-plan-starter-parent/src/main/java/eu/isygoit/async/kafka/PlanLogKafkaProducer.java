package eu.isygoit.async.kafka;

import eu.isygoit.com.event.KafkaJsonProducer;
import eu.isygoit.dto.data.LogEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Publishes job log entries to the plan-logs topic as plain JSON through {@link PlanKafkaSender} (byte[] key/value).
 * <p>
 * The stack's {@code sendAsync} casts the template result to Spring's ListenableFuture, which fails at runtime with
 * spring-kafka 3.x (KafkaTemplate returns a CompletableFuture), so the async send is implemented here.
 * <p>
 * Log publishing is best-effort: {@link #publish(LogEntry)} never throws, so a broker outage or a bad entry can never
 * fail the planning job that emitted it.
 */
@Slf4j
@Service
public class PlanLogKafkaProducer extends KafkaJsonProducer<LogEntry> {
    private static final long WARN_INTERVAL_MILLIS = 60_000L;

    private final PlanKafkaSender sender;
    private final AtomicLong lastWarnMillis = new AtomicLong(0);

    public PlanLogKafkaProducer(PlanKafkaSender sender) {
        this.sender = sender;
    }

    @Value("${spring.kafka.topics.plan-logs}")
    public void setTopic(String topic) {
        this.topic = topic;
    }

    /**
     * Publishes a log entry asynchronously (fire and forget). Never throws: any failure is logged (throttled) and the
     * entry is dropped.
     *
     * @param entry the log entry (null is ignored)
     */
    public void publish(LogEntry entry) {
        if (entry == null) {
            return;
        }
        try {
            sendAsync(entry, null);
        } catch (RuntimeException exception) {
            warnThrottled("Could not publish plan log entry to topic {} (best-effort, dropped): {}",
                    exception.getMessage());
        }
    }

    @Override
    public void sendAsync(LogEntry message, Map<String, String> customHeaders) {
        if (message == null) {
            throw new IllegalArgumentException("Log entry cannot be null");
        }
        if (topic == null || topic.isBlank()) {
            throw new IllegalStateException("Plan log Kafka topic is not configured");
        }

        byte[] payload;
        try {
            payload = serialize(message);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize log entry", exception);
        }

        CompletableFuture<?> future;
        try {
            future = sender.send(topic, null, payload);
        } catch (RuntimeException exception) {
            warnThrottled("Could not enqueue plan log entry to topic {} (best-effort, dropped): {}", exception.getMessage());
            return;
        }
        future.whenComplete((result, failure) -> {
            if (failure != null) {
                warnThrottled("Failed to publish plan log entry to topic {} (best-effort, dropped): {}", failure.getMessage());
            }
        });
    }

    /** Warns at most once per minute (debug otherwise) so a broker outage cannot flood the log. */
    private void warnThrottled(String message, String reason) {
        long now = System.currentTimeMillis();
        long last = lastWarnMillis.get();
        if (now - last >= WARN_INTERVAL_MILLIS && lastWarnMillis.compareAndSet(last, now)) {
            log.warn(message, topic, reason);
        } else {
            log.debug(message, topic, reason);
        }
    }
}
