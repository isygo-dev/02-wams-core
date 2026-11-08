package eu.isygoit.async.kafka;

import eu.isygoit.com.event.KafkaJsonProducer;
import eu.isygoit.plan.api.PlanningRequest;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class PlanJobKafkaProducer extends KafkaJsonProducer<PlanningRequest> {
    @Value("${kafka.security.enable-hmac:false}")
    private boolean hmacEnabled;

    @Value("${kafka.security.enable-encryption:false}")
    private boolean encryptionEnabled;

    @Value("${spring.kafka.topics.plan-jobs}")
    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void sendAndWait(PlanningRequest request) {
        if (request == null || request.meta() == null || request.meta().jobId() == null) {
            throw new IllegalArgumentException("A planning request with a job ID is required");
        }
        if (topic == null || topic.isBlank()) {
            throw new IllegalStateException("Plan job Kafka topic is not configured");
        }
        if (hmacEnabled || encryptionEnabled) {
            throw new IllegalStateException(
                    "Plan job publishing does not support Kafka payload signing or encryption");
        }
        KafkaTemplate<String, byte[]> template = kafkaTemplate;
        if (template == null) {
            throw new IllegalStateException("Kafka template is not configured");
        }

        byte[] payload;
        try {
            payload = serialize(request);
        } catch (Exception exception) {
            throw new KafkaException("Could not serialize planning request", exception);
        }

        ProducerRecord<String, byte[]> record =
                new ProducerRecord<>(topic, request.meta().jobId().toString(), payload);
        try {
            CompletableFuture<?> delivery = template.send(record);
            delivery.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new KafkaException("Interrupted while publishing planning request", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new KafkaException("Could not publish planning request", exception);
        }
    }
}
