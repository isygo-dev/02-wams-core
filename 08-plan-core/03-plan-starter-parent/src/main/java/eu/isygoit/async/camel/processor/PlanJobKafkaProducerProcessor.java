package eu.isygoit.async.camel.processor;

import eu.isygoit.async.kafka.PlanJobKafkaProducer;
import eu.isygoit.plan.api.PlanningRequest;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Component
public class PlanJobKafkaProducerProcessor implements Processor {
    private final PlanJobKafkaProducer producer;

    public PlanJobKafkaProducerProcessor(PlanJobKafkaProducer producer) {
        this.producer = producer;
    }

    @Override
    public void process(Exchange exchange) {
        producer.sendAndWait(exchange.getIn().getBody(PlanningRequest.class));
    }
}
