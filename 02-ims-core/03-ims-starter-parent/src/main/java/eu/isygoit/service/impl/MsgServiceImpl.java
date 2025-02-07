package eu.isygoit.service.impl;

import eu.isygoit.api.MailMessageControllerApi;
import eu.isygoit.com.camel.repository.ICamelRepository;
import eu.isygoit.dto.data.MailMessageDto;
import eu.isygoit.service.IMsgService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * The type Msg service.
 */
@Slf4j
@Service
public class MsgServiceImpl implements IMsgService {

    private final MailMessageControllerApi messageService;

    private final ICamelRepository camelRepository;

    @Autowired
    public MsgServiceImpl(MailMessageControllerApi messageService, ICamelRepository camelRepository) {
        this.messageService = messageService;
        this.camelRepository = camelRepository;
    }


    @Override
    public void sendMessage(String senderDomainName, MailMessageDto mailMessage, boolean async) {
        if (async) {
            camelRepository.asyncSendBody(ICamelRepository.send_email_queue, mailMessage);
        } else {
            this.messageService.sendMail(senderDomainName, mailMessage.getTemplateName(), mailMessage);
        }
    }
}
