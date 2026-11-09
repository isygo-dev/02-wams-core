package eu.isygoit.async.camel.processor;

import org.apache.camel.Exchange;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.component.kafka.consumer.KafkaManualCommit;

/** Manual offset commit shared by the plan consumers and their dead-letter handlers. */
final class PlanKafkaCommit {
    private PlanKafkaCommit() {
    }

    static void commit(Exchange exchange) {
        KafkaManualCommit manualCommit =
                exchange.getIn().getHeader(KafkaConstants.MANUAL_COMMIT, KafkaManualCommit.class);
        if (manualCommit == null) {
            throw new IllegalStateException("Kafka manual-commit handle is missing");
        }
        manualCommit.commit();
    }
}
