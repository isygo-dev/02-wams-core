package eu.isygoit.service.impl;

import eu.isygoit.annotation.SrvRepo;
import eu.isygoit.com.rest.service.impl.CrudService;
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

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The type Chat message service.
 */
@Slf4j
@Service
@Transactional
@SrvRepo(value = ChatMessageRepository.class)
public class ChatMessageService extends CrudService<Long, ChatMessage, ChatMessageRepository>
        implements IChatMessageService {

    @Override
    public List<WsConnectDto> getConnectionsByDomain(Long domainId) {
        return WsChannelInterceptor.getConnectionsByDomain(domainId);
    }

    @Override
    public List<ChatMessage> findByReceiverId(Long receiverId, Pageable pageable) {
        return repository().findByReceiverId(receiverId, pageable);
    }

    @Override
    public List<ChatMessage> findByReceiverIdAndSenderId(Long receiverId, Long senderId, Pageable pageable) {
        return repository().findChatStack(receiverId, senderId);
    }

    @Override
    public List<ChatAccountDto> getChatAccounts(Long userId, Pageable pageable) {
        var list = repository().findByReceiverIdOrSenderIdOrderByDateDesc(userId, userId);
        if (CollectionUtils.isEmpty(list)) {
            return List.of();
        }

        // Group by groupKey and map to ChatAccountDto
        var chatMessages = list.stream()
                .collect(Collectors.groupingBy(ChatMessage::getGroupKey))
                .values()
                .stream()
                .map(chats -> {
                    var firstChat = chats.get(0);
                    return ChatAccountDto.builder()
                            .SenderId(firstChat.getSenderId())
                            .fromFullName(firstChat.getSenderName())
                            .receiverId(firstChat.getReceiverId())
                            .date(firstChat.getDate())
                            .lastMessage(firstChat.getMessage())
                            .read(firstChat.getRead())
                            .chatStatus(WsChannelInterceptor.getStatus(userId))
                            .build();
                })
                .collect(Collectors.toUnmodifiableList());

        // Sort messages in descending order based on date
        chatMessages.sort(Comparator.comparing(ChatAccountDto::getDate, Comparator.reverseOrder()));
        return (List<ChatAccountDto>) chatMessages;
    }
}