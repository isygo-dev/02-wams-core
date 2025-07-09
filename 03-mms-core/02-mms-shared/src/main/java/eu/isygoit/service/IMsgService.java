package eu.isygoit.service;

import eu.isygoit.dto.data.MailMessageDto;

/**
 * The interface Msg service.
 */
public interface IMsgService {

    /**
     * Send message.
     *
     * @param senderDomainName the sender tenant name
     * @param mailMessage      the mail message
     * @param async            the async
     */
    void sendMessage(String senderDomainName, MailMessageDto mailMessage, boolean async);
}
