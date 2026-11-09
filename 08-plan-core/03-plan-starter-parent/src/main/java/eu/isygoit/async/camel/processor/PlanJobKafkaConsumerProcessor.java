package eu.isygoit.async.camel.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.dto.tech.PlanJobMessage;
import eu.isygoit.service.impl.JobProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.kafka.KafkaConstants;
import org.springframework.stereotype.Component;

/**
 * Consumes one {@link PlanJobMessage} per exchange: the job id is read from the JSON body (when the body is blank, the
 * record key, which is the job id as a string, is used instead) and the persisted request is processed by
 * {@link JobProcessor#process(Long)}. The Kafka offset is committed manually ONLY after the processor returned
 * (terminal state persisted). Failures propagate to the route onException blocks (bounded retry, then dead-letter
 * topic + commit, see plan-job-camel-routes.xml); a missing/invalid job id is a {@link PoisonMessageException}.
 */
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
        Long jobId = readJobId(exchange);
        if (jobId == null) {
            throw new PoisonMessageException("Plan job message has no job id");
        }
        jobProcessor.process(jobId);

        PlanKafkaCommit.commit(exchange);
    }

    private Long readJobId(Exchange exchange) throws Exception {
        String body = exchange.getIn().getBody(String.class);
        if (body == null || body.isBlank()) {
            return PlanKafkaKeys.jobIdOf(PlanKafkaKeys.keyOf(exchange.getIn().getHeader(KafkaConstants.KEY)));
        }
        PlanJobMessage message = objectMapper.readValue(body, PlanJobMessage.class);
        return message == null ? null : message.jobId();
    }
}
