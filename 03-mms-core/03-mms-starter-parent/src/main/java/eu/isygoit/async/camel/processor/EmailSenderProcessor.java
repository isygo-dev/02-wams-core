package eu.isygoit.async.camel.processor;

import eu.isygoit.api.MailMessageControllerApi;
import eu.isygoit.com.camel.processor.AbstractCamelProcessor;
import eu.isygoit.dto.data.MailMessageDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * The type Email sender processor.
 */
@Slf4j
@Component
@Qualifier("emailSenderProcessor")
public class EmailSenderProcessor extends AbstractCamelProcessor<MailMessageDto> {

    @Autowired
    private MailMessageControllerApi messageService;

    @Override
    public void performProcessor(Exchange exchange, MailMessageDto mailMessageDto) throws Exception {
        exchange.getIn().setHeader("toEmail", mailMessageDto.getToAddr());
        exchange.getIn().setHeader("subject", mailMessageDto.getSubject());
        this.messageService.sendMail(
                mailMessageDto.getTenant(), mailMessageDto.getTemplateName(), mailMessageDto);
        exchange.getIn().setHeader(AbstractCamelProcessor.RETURN_HEADER, true);
    }
}
