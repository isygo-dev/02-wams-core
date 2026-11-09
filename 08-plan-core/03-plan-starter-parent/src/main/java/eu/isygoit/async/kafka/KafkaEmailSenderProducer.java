package eu.isygoit.async.kafka;

import eu.isygoit.com.event.KafkaJsonProducer;
import eu.isygoit.dto.data.MailMessageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * The type Kafka email sender producer.
 */
@Slf4j
@Service
public class KafkaEmailSenderProducer extends KafkaJsonProducer<MailMessageDto> {

    @Value("${spring.kafka.topics.send-email}")
    public void setTopic(String topic) {
        this.topic = topic;
    }
}
