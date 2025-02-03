package eu.isygoit.config;

import eu.isygoit.com.camel.repository.ICamelRepository;
import eu.isygoit.dto.wsocket.WsConnectDto;
import eu.isygoit.dto.wsocket.WsMessageDto;
import eu.isygoit.dto.wsocket.WsMessageWrapperDto;
import eu.isygoit.dto.wsocket.WsSubscribeDto;
import eu.isygoit.enums.IEnumWSBroker;
import eu.isygoit.enums.IEnumWSEndpoint;
import eu.isygoit.enums.IEnumWSMessage;
import eu.isygoit.enums.IEnumWSStatus;
import eu.isygoit.service.IWebSocketService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The type Ws channel interceptor.
 */
@Slf4j
@Component
public class WsChannelInterceptor implements ChannelInterceptor {

    private static final StringMessageConverter stringMessageConverter = new StringMessageConverter();
    private static final Map<String, WsConnectDto> connectedUsers = new HashMap<>();

    @Autowired
    private ICamelRepository camelRepository;

    @Autowired
    private IWebSocketService webSocketService;

    /**
     * Gets connections by domain.
     *
     * @param domainId the domain id
     * @return the connections by domain
     */
    public static List<WsConnectDto> getConnectionsByDomain(Long domainId) {
        if (!CollectionUtils.isEmpty(connectedUsers)) {
            return connectedUsers.values().stream().filter(wsConnectDto -> wsConnectDto.getGroupId().equals(domainId)).collect(Collectors.toUnmodifiableList());
        }
        return Collections.emptyList();
    }

    /**
     * Gets status.
     *
     * @param userId the user id
     * @return the status
     */
    public static IEnumWSStatus.Types getStatus(Long userId) {
        Optional<WsConnectDto> optional = connectedUsers.values().stream().parallel().filter(wsc -> wsc.getSenderId().equals(userId)).findFirst();
        if (optional.isPresent()) {
            return optional.get().getStatus();
        }
        return IEnumWSStatus.Types.DISCONNECTED;
    }

    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        ChannelInterceptor.super.postSend(message, channel, sent);
    }

    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
        ChannelInterceptor.super.afterSendCompletion(message, channel, sent, ex);
    }

    @SneakyThrows
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor stompHeaderAccessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        //get header variables
        Object sessionIdObject = stompHeaderAccessor.getHeader("simpSessionId");
        if (Objects.isNull(sessionIdObject)) {
            throw new SessionAuthenticationException("sessionId not provided");
        }
        String sessionId = (String) sessionIdObject;
        WsConnectDto wsConnect = connectedUsers.get(sessionId);
        log.info("WS Session id not connected: {}", sessionId);

        if (StompCommand.STOMP.equals(stompHeaderAccessor.getCommand())) {
            log.info("presend >>> NOT IMPLEMENTED >>> {} / {}", stompHeaderAccessor.getCommand().name(), message);
        } else if (StompCommand.CONNECT.equals(stompHeaderAccessor.getCommand())) {
            log.info("presend >>>  {} / {}", stompHeaderAccessor.getCommand().name(), message);
            Long senderId = Long.valueOf(stompHeaderAccessor.getNativeHeader("senderId").get(0));
            Long groupId = Long.valueOf(stompHeaderAccessor.getNativeHeader("groupId").get(0));
            processWsConnect(sessionId, senderId, groupId);
        } else if (StompCommand.DISCONNECT.equals(stompHeaderAccessor.getCommand())) {
            log.info("presend >>>  {} / {}", stompHeaderAccessor.getCommand().name(), message);
            Assert.notNull(wsConnect, sessionId);
            processWsDisconnect(wsConnect);
        } else if (StompCommand.SUBSCRIBE.equals(stompHeaderAccessor.getCommand())) {
            log.info("presend >>>  {} / {}", stompHeaderAccessor.getCommand().name(), message);
            Assert.notNull(wsConnect, sessionId);
            processWsSubscribe(stompHeaderAccessor.getHeader("simpDestination").toString(), wsConnect);
        } else if (StompCommand.UNSUBSCRIBE.equals(stompHeaderAccessor.getCommand())) {
            log.info("presend >>>  {} / {}", stompHeaderAccessor.getCommand().name(), message);
            Assert.notNull(wsConnect, sessionId);
            processWsUnsubscribe(stompHeaderAccessor.getHeader("simpDestination").toString(), wsConnect);
        } else if (StompCommand.SEND.equals(stompHeaderAccessor.getCommand())) {
            log.info("presend >>>  {} / {}", stompHeaderAccessor.getCommand().name(), message);
            Assert.notNull(wsConnect, sessionId);
            processWsSend((String) stringMessageConverter.fromMessage(message, String.class), Long.valueOf(stompHeaderAccessor.getHeader("simpDestination").toString()), wsConnect);
        } else if (StompCommand.ACK.equals(stompHeaderAccessor.getCommand())) {
            log.info("presend >>> NOT IMPLEMENTED >>> {} / {}", stompHeaderAccessor.getCommand().name(), message);
        } else if (StompCommand.NACK.equals(stompHeaderAccessor.getCommand())) {
            log.info("presend >>> NOT IMPLEMENTED >>> {} / {}", stompHeaderAccessor.getCommand().name(), message);
        } else if (StompCommand.BEGIN.equals(stompHeaderAccessor.getCommand())) {
            log.info("presend >>> NOT IMPLEMENTED >>> {} / {}", stompHeaderAccessor.getCommand().name(), message);
        } else if (StompCommand.COMMIT.equals(stompHeaderAccessor.getCommand())) {
            log.info("presend >>> NOT IMPLEMENTED >>> {} / {}", stompHeaderAccessor.getCommand().name(), message);
        } else if (StompCommand.ABORT.equals(stompHeaderAccessor.getCommand())) {
            log.info("presend >>> NOT IMPLEMENTED >>> {} / {}", stompHeaderAccessor.getCommand().name(), message);
        } else if (StompCommand.CONNECTED.equals(stompHeaderAccessor.getCommand())) {
            log.info("presend >>> NOT IMPLEMENTED >>> {} / {}", stompHeaderAccessor.getCommand().name(), message);
        } else if (StompCommand.RECEIPT.equals(stompHeaderAccessor.getCommand())) {
            log.info("presend >>> NOT IMPLEMENTED >>> {} / {}", stompHeaderAccessor.getCommand().name(), message);
        } else if (StompCommand.MESSAGE.equals(stompHeaderAccessor.getCommand())) {
            log.info("presend >>> NOT IMPLEMENTED >>> {} / {}", stompHeaderAccessor.getCommand().name(), message);
        } else if (StompCommand.ERROR.equals(stompHeaderAccessor.getCommand())) {
            log.info("presend >>> NOT IMPLEMENTED >>> {} / {}", stompHeaderAccessor.getCommand().name(), message);
        }
        return message;
    }

    private void processWsSend(String message, Long recieverId, WsConnectDto wsConnect) throws IOException {
        webSocketService.saveAndSendToUser(recieverId,
                WsMessageWrapperDto.builder()
                        .endPoint(IEnumWSEndpoint.Types.CHAT)
                        .broker(IEnumWSBroker.Types.USER)
                        .senderId(wsConnect.getSenderId())
                        .message(WsMessageDto.builder()
                                .type(IEnumWSMessage.Types.MESSAGE)
                                .content(message)
                                .build())
                        .build());
    }

    private void processWsUnsubscribe(String subscribeDestination, WsConnectDto wsConnect) {
        IEnumWSEndpoint.Types subscribeEndpoint = IEnumWSEndpoint.getEndPoint(subscribeDestination);
        Assert.notNull(subscribeEndpoint, "No endpoint type for:" + subscribeDestination);
        IEnumWSBroker.Types subscribeBroker = IEnumWSBroker.getBroker(subscribeDestination);
        Assert.notNull(subscribeEndpoint, "No broker type for:" + subscribeDestination);
        wsConnect.setStatus(IEnumWSStatus.Types.DO_NOT_DISTURB);
        camelRepository.asyncSendBody(ICamelRepository.subscribe_chat_queue,
                WsSubscribeDto.builder()
                        .sessionId(wsConnect.getSessionId())
                        .senderId(wsConnect.getSenderId())
                        .groupId(wsConnect.getGroupId())
                        .endPoint(subscribeEndpoint)
                        .broker(subscribeBroker)
                        .status(IEnumWSStatus.Types.DO_NOT_DISTURB)
                        .build());
    }

    private void processWsSubscribe(String subscribeDestination, WsConnectDto wsConnect) {
        IEnumWSEndpoint.Types subscribeEndpoint = IEnumWSEndpoint.getEndPoint(subscribeDestination);
        Assert.notNull(subscribeEndpoint, "No endpoint type for:" + subscribeDestination);
        IEnumWSBroker.Types subscribeBroker = IEnumWSBroker.getBroker(subscribeDestination);
        Assert.notNull(subscribeEndpoint, "No broker type for:" + subscribeDestination);
        wsConnect.setStatus(IEnumWSStatus.Types.CONNECTED);
        camelRepository.asyncSendBody(ICamelRepository.subscribe_chat_queue,
                WsSubscribeDto.builder()
                        .sessionId(wsConnect.getSessionId())
                        .senderId(wsConnect.getSenderId())
                        .groupId(wsConnect.getGroupId())
                        .endPoint(subscribeEndpoint)
                        .broker(subscribeBroker)
                        .status(IEnumWSStatus.Types.CONNECTED)
                        .build());
    }

    private void processWsDisconnect(WsConnectDto wsConnect) {
        wsConnect = connectedUsers.remove(wsConnect.getSessionId());
        wsConnect.setStatus(IEnumWSStatus.Types.DISCONNECTED);
        camelRepository.asyncSendBody(ICamelRepository.connect_chat_queue, wsConnect);
    }

    private void processWsConnect(String sessionId, Long senderId, Long groupId) {
        WsConnectDto wsConnect = WsConnectDto.builder()
                .sessionId(sessionId)
                .senderId(senderId)
                .groupId(groupId)
                .status(IEnumWSStatus.Types.DO_NOT_DISTURB)
                .build();
        //Clean the connected users map
        connectedUsers.values().removeIf(wsConnectDto -> wsConnectDto.getSenderId().equals(senderId));
        connectedUsers.put(wsConnect.getSessionId(), wsConnect);
        camelRepository.asyncSendBody(ICamelRepository.connect_chat_queue, wsConnect);
    }
}
