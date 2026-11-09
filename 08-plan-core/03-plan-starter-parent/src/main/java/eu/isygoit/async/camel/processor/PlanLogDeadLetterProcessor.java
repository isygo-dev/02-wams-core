package eu.isygoit.async.camel.processor;

import eu.isygoit.async.kafka.PlanDeadLetterProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Dead-letter handler of the plan-log consumer route. Log persistence is best-effort: if even the dead-letter publish
 * fails the record is dropped (logged at error) and the offset is committed so logs never stall.
 */
@Component
public class PlanLogDeadLetterProcessor extends AbstractPlanDeadLetterProcessor {
    public PlanLogDeadLetterProcessor(
            PlanDeadLetterProducer deadLetterProducer,
            @Value("${spring.kafka.topics.plan-logs-dlq:isygo.plan.logs.dlq}") String deadLetterTopic) {
        super(deadLetterProducer, deadLetterTopic, true);
    }
}
