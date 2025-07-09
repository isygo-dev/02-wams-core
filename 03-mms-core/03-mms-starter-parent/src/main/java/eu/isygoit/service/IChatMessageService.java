package eu.isygoit.service;

import eu.isygoit.com.rest.service.ICrudServiceMethods;
import eu.isygoit.com.rest.service.tenancy.ICrudTenantServiceMethods;
import eu.isygoit.dto.data.ChatAccountDto;
import eu.isygoit.dto.wsocket.WsConnectDto;
import eu.isygoit.model.ChatMessage;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;


/**
 * The interface Chat message service.
 */
public interface IChatMessageService extends ICrudServiceMethods<UUID, ChatMessage> {

    /**
     * Find by receiver id list.
     *
     * @param receiverId the receiver id
     * @param pageable   the pageable
     * @return the list
     */
    List<ChatMessage> findByReceiverId(Long receiverId, Pageable pageable);

    /**
     * Find by receiver id and sender id list.
     *
     * @param receiverId the receiver id
     * @param senderId   the sender id
     * @param pageable   the pageable
     * @return the list
     */
    List<ChatMessage> findByReceiverIdAndSenderId(Long receiverId, Long senderId, Pageable pageable);

    /**
     * Gets chat accounts.
     *
     * @param userId   the user id
     * @param pageable the pageable
     * @return the chat accounts
     */
    List<ChatAccountDto> getChatAccounts(Long userId, Pageable pageable);

    /**
     * Gets connections by tenant.
     *
     * @param tenantId the tenant id
     * @return the connections by tenant
     */
    List<WsConnectDto> getConnectionsByTenant(Long tenantId);
}
