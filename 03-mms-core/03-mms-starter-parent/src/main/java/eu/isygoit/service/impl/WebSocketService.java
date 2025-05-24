package eu.isygoit.service.impl;

import eu.isygoit.annotation.ServRepo;
import eu.isygoit.com.camel.repository.ICamelRepository;
import eu.isygoit.com.rest.service.CrudServiceUtils;
import eu.isygoit.dto.wsocket.WsMessageWrapperDto;
import eu.isygoit.enums.IEnumWSEndpoint;
import eu.isygoit.model.ChatMessage;
import eu.isygoit.repository.ChatMessageRepository;
import eu.isygoit.service.IChatMessageService;
import eu.isygoit.service.IWebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Date;

/**
 * The type Web socket service.
 */
@Slf4j
@Service
@Transactional
@ServRepo(value = ChatMessageRepository.class)
public class WebSocketService extends CrudServiceUtils<Long, ChatMessage, ChatMessageRepository>
        implements IWebSocketService {

    @Autowired
    private IChatMessageService chatMessageService;

    @Autowired
    private ICamelRepository camelRepository;

    @Override
    public void saveAndSendToUser(Long recieverId, WsMessageWrapperDto message) throws IOException {
        if (IEnumWSEndpoint.Types.CHAT == message.getEndPoint()) {
            chatMessageService.create(ChatMessage.builder()
                    .receiverId(recieverId)
                    .senderId(message.getSenderId())

                    .message(message.getMessage().getContent())
                    .date(new Date())
                    .build());
        }

        camelRepository.asyncRequestBodyAndHeader(ICamelRepository.send_socket_queue, message, "receiverId", recieverId);
    }

    @Override
    public void saveAndSendToGroup(Long groupId, WsMessageWrapperDto message) {
        //TODO -- Split the message by user and save before
        camelRepository.asyncRequestBodyAndHeader(ICamelRepository.send_socket_queue, message, "receiverId", groupId);
    }

    @Override
    public void saveAndSendToAll(WsMessageWrapperDto message) {
        //TODO -- Split the message by user and save before
        camelRepository.asyncSendBody(ICamelRepository.send_socket_queue, message);
    }
}
