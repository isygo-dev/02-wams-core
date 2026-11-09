package eu.isygoit.async.camel.processor;

import eu.isygoit.api.MailMessageServiceApi;
import eu.isygoit.async.kafka.KafkaEmailSenderProducer;
import eu.isygoit.com.camel.processor.AbstractCamelProcessor;
import eu.isygoit.dto.data.MailMessageDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The type Email sender processor.
 */
@Slf4j
@Component
@Qualifier("emailSenderProcessor")
public class EmailSenderProcessor extends AbstractCamelProcessor<MailMessageDto> {

    @Value("${app.email.broker}")
    private String throughBroker;

    @Autowired
    private KafkaEmailSenderProducer kafkaEmailSenderProducer;

    @Autowired
    private MailMessageServiceApi messageService;

    @Override
    public void performProcessor(Exchange exchange, MailMessageDto mailMessageDto) throws Exception {
        exchange.getIn().setHeader("toEmail", mailMessageDto.getToAddr());
        exchange.getIn().setHeader("subject", mailMessageDto.getSubject());
        if (MailMessageDto.THROUGH_KAFKA.equals(throughBroker)) {
            kafkaEmailSenderProducer.send(mailMessageDto);
        } else if (MailMessageDto.THROUGH_REST.equals(throughBroker)) {
            this.messageService.sendMail(mailMessageDto.getTemplateName(), mailMessageDto);
        } else {
            this.messageService.sendMail(mailMessageDto.getTemplateName(), mailMessageDto);
        }
        exchange.getIn().setHeader(AbstractCamelProcessor.RETURN_HEADER, true);
    }
}
