package eu.isygoit.service;

import eu.isygoit.dto.data.MailMessageDto;

/**
 * The interface Msg service.
 */
public interface IMsgService {

    /**
     * Send message.
     *
     * @param senderTenantName the sender tenant name
     * @param mailMessage      the mail message
     * @param async            the async
     */
    void sendMessage(String senderTenantName, MailMessageDto mailMessage, boolean async);
}
