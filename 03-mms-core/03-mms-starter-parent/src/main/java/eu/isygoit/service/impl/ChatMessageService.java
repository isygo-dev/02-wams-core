package eu.isygoit.service.impl;

import eu.isygoit.annotation.InjectRepository;
import eu.isygoit.com.rest.service.cassandra.CassandraCrudService;
import eu.isygoit.config.WsChannelInterceptor;
import eu.isygoit.dto.data.ChatAccountDto;
import eu.isygoit.dto.wsocket.WsConnectDto;
import eu.isygoit.model.ChatMessage;
import eu.isygoit.repository.ChatMessageRepository;
import eu.isygoit.service.IChatMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The type Chat message service.
 */
@Slf4j
@Service
@Transactional
@InjectRepository(value = ChatMessageRepository.class)
public class ChatMessageService extends CassandraCrudService<UUID, ChatMessage, ChatMessageRepository>
        implements IChatMessageService {

    public List<WsConnectDto> getConnectionsByTenant(Long tenantId) {
        return WsChannelInterceptor.getConnectionsByTenant(tenantId);
    }

    @Override
    public List<ChatMessage> findByReceiverId(Long receiverId, Pageable pageable) {
        return repository().findByReceiverId(receiverId, pageable);
    }

    @Override
    public List<ChatMessage> findByReceiverIdAndSenderId(Long receiverId, Long senderId, Pageable pageable) {
        List<ChatMessage> messages1 = repository().findByReceiverIdAndSenderId(receiverId, senderId);
        List<ChatMessage> messages2 = repository().findByReceiverIdAndSenderId(senderId, receiverId);

        List<ChatMessage> allMessages = new ArrayList<>();
        allMessages.addAll(messages1);
        allMessages.addAll(messages2);

        // Sort messages by date
        allMessages.sort(Comparator.comparing(ChatMessage::getDate));
        return allMessages;
    }

    @Override
    public List<ChatAccountDto> getChatAccounts(Long userId, Pageable pageable) {
        List<ChatMessage> list = repository().findByReceiverIdOrSenderIdOrderByDateDesc(userId, userId);
        if (!CollectionUtils.isEmpty(list)) {
            List<ChatAccountDto> chatMessages = list.stream().collect(Collectors.groupingBy(ChatMessage::getGroupKey))
                    .values().stream().map(chats ->
                            ChatAccountDto.builder()
                                    .senderId(chats.get(0).getSenderId())
                                    .fromFullName(chats.get(0).getSenderName())
                                    .receiverId(chats.get(0).getReceiverId())
                                    .date(chats.get(0).getDate())
                                    .lastMessage(chats.get(0).getMessage())
                                    .read(chats.get(0).getRead())
                                    .chatStatus(WsChannelInterceptor.getStatus(userId))
                                    .build()
                    ).collect(Collectors.toList());

            chatMessages.sort((o1, o2) -> o2.getDate().compareTo(o1.getDate()));
            return chatMessages;
        }

        return Collections.EMPTY_LIST;
    }
}
