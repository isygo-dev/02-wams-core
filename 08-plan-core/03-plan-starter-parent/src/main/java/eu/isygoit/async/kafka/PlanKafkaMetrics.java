package eu.isygoit.async.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Optional Micrometer counters for the plan Kafka traffic: plan.kafka.sent, plan.kafka.send.failed and
 * plan.kafka.dlq, all tagged with the topic. Without a MeterRegistry every call is a no-op.
 */
@Component
public class PlanKafkaMetrics {
    private final Supplier<MeterRegistry> registry;

    @Autowired
    public PlanKafkaMetrics(ObjectProvider<MeterRegistry> registryProvider) {
        this.registry = registryProvider::getIfAvailable;
    }

    public PlanKafkaMetrics(MeterRegistry meterRegistry) {
        this.registry = () -> meterRegistry;
    }

    public void sent(String topic) {
        increment("plan.kafka.sent", topic);
    }

    public void sendFailed(String topic) {
        increment("plan.kafka.send.failed", topic);
    }

    public void deadLettered(String topic) {
        increment("plan.kafka.dlq", topic);
    }

    private void increment(String name, String topic) {
        try {
            MeterRegistry meterRegistry = registry.get();
            if (meterRegistry != null) {
                meterRegistry.counter(name, "topic", topic == null ? "unknown" : topic).increment();
            }
        } catch (RuntimeException ignored) {
            // metrics must never affect the messaging path
        }
    }
}
