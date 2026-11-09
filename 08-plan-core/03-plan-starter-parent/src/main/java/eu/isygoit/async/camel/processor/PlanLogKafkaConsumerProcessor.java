package eu.isygoit.async.camel.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.dto.data.LogEntry;
import eu.isygoit.service.IPlanLogService;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

/**
 * Persists one log entry per exchange through {@link IPlanLogService#append(LogEntry)} (which applies the retention
 * TTL on write) and commits the offset only after the write succeeded. Failures propagate to the route onException
 * blocks (bounded retry, then logs dead-letter topic + commit, see plan-log-camel-routes.xml).
 * Future improvement: batch the writes (not implemented, one synchronous append per record today).
 */
@Component
public class PlanLogKafkaConsumerProcessor implements Processor {
    private final IPlanLogService planLogService;
    private final ObjectMapper objectMapper;

    public PlanLogKafkaConsumerProcessor(IPlanLogService planLogService, ObjectMapper objectMapper) {
        this.planLogService = planLogService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getIn().getBody(String.class);
        if (body == null || body.isBlank()) {
            throw new PoisonMessageException("Empty plan log message");
        }
        LogEntry entry = objectMapper.readValue(body, LogEntry.class);
        planLogService.append(entry);

        PlanKafkaCommit.commit(exchange);
    }
}
