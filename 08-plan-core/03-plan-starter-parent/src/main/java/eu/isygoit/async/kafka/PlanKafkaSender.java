package eu.isygoit.async.kafka;

import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Dedicated byte[]/byte[] Kafka sender for the plan topics.
 * <p>
 * It forces ByteArraySerializer for key and value regardless of the spring.kafka.producer.* serializer settings, so
 * the plan producers never depend on yml defaults or on the stack's shared KafkaTemplate generic types. Connection
 * settings (bootstrap servers, security, ...) still come from spring.kafka.*. The factory is deliberately not
 * exposed as a bean so Spring Boot's own ProducerFactory/KafkaTemplate stay available to other components.
 * <p>
 * max.block.ms defaults to app.plan.kafka.max-block-ms (5000) unless set explicitly in spring.kafka.producer.properties,
 * so a broker outage cannot block the calling (solve) thread for the Kafka default of 60 seconds.
 */
@Component
public class PlanKafkaSender {
    private final DefaultKafkaProducerFactory<byte[], byte[]> producerFactory;
    private final KafkaTemplate<byte[], byte[]> template;
    private final PlanKafkaMetrics metrics;

    public PlanKafkaSender(
            KafkaProperties kafkaProperties,
            PlanKafkaMetrics metrics,
            @Value("${app.plan.kafka.max-block-ms:5000}") long maxBlockMs) {
        this.metrics = metrics;
        Map<String, Object> props = kafkaProperties.buildProducerProperties(null);
        props.put("key.serializer", ByteArraySerializer.class);
        props.put("value.serializer", ByteArraySerializer.class);
        props.putIfAbsent("max.block.ms", maxBlockMs);
        this.producerFactory = new DefaultKafkaProducerFactory<>(props);
        this.template = new KafkaTemplate<>(producerFactory);
    }

    public CompletableFuture<SendResult<byte[], byte[]>> send(String topic, String key, byte[] payload) {
        return send(topic, key, payload, null);
    }

    /**
     * Sends a record with optional string headers (used by the dead-letter producer). Increments the
     * plan.kafka.sent / plan.kafka.send.failed counters once the broker outcome is known.
     */
    public CompletableFuture<SendResult<byte[], byte[]>> send(
            String topic, String key, byte[] payload, Map<String, String> headers) {
        byte[] keyBytes = key == null ? null : key.getBytes(StandardCharsets.UTF_8);
        List<Header> recordHeaders = new ArrayList<>();
        if (headers != null) {
            headers.forEach((name, value) -> {
                if (name != null && value != null) {
                    recordHeaders.add(new RecordHeader(name, value.getBytes(StandardCharsets.UTF_8)));
                }
            });
        }
        CompletableFuture<SendResult<byte[], byte[]>> future;
        try {
            future = template.send(new ProducerRecord<>(topic, null, null, keyBytes, payload, recordHeaders));
        } catch (RuntimeException exception) {
            metrics.sendFailed(topic);
            throw exception;
        }
        return future.whenComplete((result, failure) -> {
            if (failure == null) {
                metrics.sent(topic);
            } else {
                metrics.sendFailed(topic);
            }
        });
    }

    @PreDestroy
    void close() {
        producerFactory.destroy();
    }
}
