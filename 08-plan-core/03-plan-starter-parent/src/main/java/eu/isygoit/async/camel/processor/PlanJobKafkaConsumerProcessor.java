package eu.isygoit.async.camel.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.plan.api.PlanningRequest;
import eu.isygoit.plan.application.JobProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.component.kafka.consumer.KafkaManualCommit;
import org.springframework.stereotype.Component;

@Component
public class PlanJobKafkaConsumerProcessor implements Processor {
    private final JobProcessor jobProcessor;
    private final ObjectMapper objectMapper;

    public PlanJobKafkaConsumerProcessor(JobProcessor jobProcessor, ObjectMapper objectMapper) {
        this.jobProcessor = jobProcessor;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        PlanningRequest request = objectMapper.readValue(
                exchange.getIn().getBody(String.class), PlanningRequest.class);
        jobProcessor.process(request);

        KafkaManualCommit manualCommit =
                exchange.getIn().getHeader(KafkaConstants.MANUAL_COMMIT, KafkaManualCommit.class);
        if (manualCommit == null) {
            throw new IllegalStateException("Kafka manual-commit handle is missing");
        }
        manualCommit.commit();
    }
}
