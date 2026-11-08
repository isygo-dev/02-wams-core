package eu.isygoit.async.kafka;

import eu.isygoit.com.event.KafkaJsonProducer;
import eu.isygoit.plan.domain.LogEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PlanLogKafkaProducer extends KafkaJsonProducer<LogEntry> {
    @Value("${spring.kafka.topics.plan-logs}")
    public void setTopic(String topic) {
        this.topic = topic;
    }
}
