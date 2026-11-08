package eu.isygoit.async.camel.processor;

import eu.isygoit.async.kafka.PlanLogKafkaProducer;
import eu.isygoit.plan.domain.LogEntry;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component
public class PlanLogKafkaProducerProcessor implements Processor {
    private final PlanLogKafkaProducer producer;

    public PlanLogKafkaProducerProcessor(PlanLogKafkaProducer producer) {
        this.producer = producer;
    }

    @Override
    public void process(Exchange exchange) {
        producer.sendAsync(exchange.getIn().getBody(LogEntry.class), null);
    }
}
