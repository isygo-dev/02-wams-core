package eu.isygoit.async.camel.processor;

import eu.isygoit.com.camel.processor.AbstractCamelProcessor;
import eu.isygoit.com.camel.processor.AbstractStringProcessor;
import eu.isygoit.com.camel.repository.ICamelRepository;
import eu.isygoit.dto.data.MailMessageDto;
import eu.isygoit.helper.JsonHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;


/**
 * The type Kafka email sender consumer processor.
 */
@Slf4j
@Component
@Qualifier("kafkaEmailSenderConsumerProcessor")
public class KafkaEmailSenderConsumerProcessor extends AbstractStringProcessor {

    @Autowired
    private ICamelRepository camelRepository;

    @Override
    public void performProcessor(Exchange exchange, String mailMsg) throws Exception {
        MailMessageDto mailMessage = JsonHelper.fromJson(mailMsg, MailMessageDto.class);
        camelRepository.asyncSendBody(ICamelRepository.send_email_queue, mailMessage);
        exchange.getIn().setHeader(AbstractCamelProcessor.RETURN_HEADER, true);
    }
}
