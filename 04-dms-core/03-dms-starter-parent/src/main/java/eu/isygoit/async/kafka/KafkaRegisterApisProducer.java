package eu.isygoit.async.kafka;

import eu.isygoit.com.event.KafkaJsonProducer;
import eu.isygoit.dto.extendable.ApiPermissionModelDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * The type Kafka register apis producer.
 */
@Slf4j
@Service
public class KafkaRegisterApisProducer extends KafkaJsonProducer<ApiPermissionModelDto> {

    @Value("${spring.kafka.topics.register-api-permission}")
    public void setTopic(String topic) {
        this.topic = topic;
    }
}