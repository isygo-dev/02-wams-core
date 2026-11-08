package eu.isygoit.async.camel.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.plan.domain.LogEntry;
import eu.isygoit.plan.port.LogEntryRepository;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.component.kafka.consumer.KafkaManualCommit;
import org.springframework.stereotype.Component;

@Component
public class PlanLogKafkaConsumerProcessor implements Processor {
    private final LogEntryRepository logEntryRepository;
    private final ObjectMapper objectMapper;

    public PlanLogKafkaConsumerProcessor(
            LogEntryRepository logEntryRepository,
            ObjectMapper objectMapper) {
        this.logEntryRepository = logEntryRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        LogEntry entry = objectMapper.readValue(
                exchange.getIn().getBody(String.class), LogEntry.class);
        logEntryRepository.append(entry);

        KafkaManualCommit manualCommit =
                exchange.getIn().getHeader(KafkaConstants.MANUAL_COMMIT, KafkaManualCommit.class);
        if (manualCommit == null) {
            throw new IllegalStateException("Kafka manual-commit handle is missing");
        }
        manualCommit.commit();
    }
}
