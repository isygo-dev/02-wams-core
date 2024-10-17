package eu.isygoit.service;

import eu.isygoit.dto.wsocket.WsMessageWrapperDto;

import java.io.IOException;


/**
 * The interface Web socket service.
 */
public interface IWebSocketService {

    /**
     * Save and send to user.
     *
     * @param recieverId the reciever id
     * @param message    the message
     * @throws IOException the io exception
     */
    void saveAndSendToUser(Long recieverId, WsMessageWrapperDto message) throws IOException;

    /**
     * Save and send to group.
     *
     * @param groupId the group id
     * @param message the message
     */
    void saveAndSendToGroup(Long groupId, WsMessageWrapperDto message);

    /**
     * Save and send to all.
     *
     * @param message the message
     */
    void saveAndSendToAll(WsMessageWrapperDto message);
}
