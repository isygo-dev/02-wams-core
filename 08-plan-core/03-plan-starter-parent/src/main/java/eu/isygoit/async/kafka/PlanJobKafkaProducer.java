package eu.isygoit.async.kafka;

import eu.isygoit.com.event.KafkaJsonProducer;
import eu.isygoit.dto.tech.PlanJobMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.KafkaException;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publishes a {@link PlanJobMessage} (job id + tenant, never the planning request) to the plan-jobs topic through
 * {@link PlanKafkaSender} (byte[] key/value). The record key is the job id as a UTF-8 string so a job is always routed
 * to the same partition. The call blocks until the broker acknowledges (bounded by app.plan.kafka.send-timeout-ms)
 * because the caller must know whether the job was really enqueued.
 */
@Service
public class PlanJobKafkaProducer extends KafkaJsonProducer<PlanJobMessage> {
    private final PlanKafkaSender sender;

    @Value("${kafka.security.enable-hmac:false}")
    private boolean hmacEnabled;

    @Value("${kafka.security.enable-encryption:false}")
    private boolean encryptionEnabled;

    @Value("${app.plan.kafka.send-timeout-ms:15000}")
    private long sendTimeoutMs = 15000;

    public PlanJobKafkaProducer(PlanKafkaSender sender) {
        this.sender = sender;
    }

    @Value("${spring.kafka.topics.plan-jobs}")
    public void setTopic(String topic) {
        this.topic = topic;
    }

    /**
     * Sends the message and waits for the broker acknowledgement.
     *
     * @param message the job message; its job id is required
     * @throws IllegalArgumentException if the message or its job id is missing
     * @throws IllegalStateException    if the topic is not configured or payload signing/encryption is enabled
     * @throws KafkaException           if the message cannot be serialized or published (timeout, broker failure,
     *                                  interruption)
     */
    public void dispatch(PlanJobMessage message) {
        if (message == null || message.jobId() == null) {
            throw new IllegalArgumentException("A plan job message with a job ID is required");
        }
        if (topic == null || topic.isBlank()) {
            throw new IllegalStateException("Plan job Kafka topic is not configured");
        }
        if (hmacEnabled || encryptionEnabled) {
            throw new IllegalStateException(
                    "Plan job publishing does not support Kafka payload signing or encryption");
        }

        byte[] payload;
        try {
            payload = serialize(message);
        } catch (Exception exception) {
            throw new KafkaException("Could not serialize plan job message", exception);
        }

        try {
            sender.send(topic, message.jobId().toString(), payload).get(sendTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new KafkaException("Interrupted while publishing plan job message", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new KafkaException("Could not publish plan job message", exception);
        }
    }
}
