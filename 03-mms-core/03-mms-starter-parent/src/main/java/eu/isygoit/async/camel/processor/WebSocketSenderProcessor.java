package eu.isygoit.async.camel.processor;

import eu.isygoit.com.camel.processor.AbstractCamelProcessor;
import eu.isygoit.dto.wsocket.WsMessageWrapperDto;
import eu.isygoit.enums.IEnumWSEndpoint;
import eu.isygoit.helper.JsonHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * The type Web socket sender processor.
 */
@Slf4j
@Component
@Qualifier("webSocketSenderProcessor")
public class WebSocketSenderProcessor extends AbstractCamelProcessor<WsMessageWrapperDto> {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void performProcessor(Exchange exchange, WsMessageWrapperDto wsMessage) throws Exception {
        Long receiverId = exchange.getIn().getHeader("receiverId", Long.class);
        exchange.getIn().setHeader("receiverId", receiverId);
        exchange.getIn().setHeader("senderId", wsMessage.getSenderId());

        String endpoint = new StringBuilder("/")
                .append((IEnumWSEndpoint.Types.FREE == wsMessage.getEndPoint()) ? wsMessage.getFreeEndoint() : wsMessage.getEndPoint().name().toLowerCase())
                .append("/").append(wsMessage.getBroker().name().toLowerCase())
                .append("/").append(receiverId)
                .toString();  //chat/user/124

        switch (wsMessage.getBroker()) {
            case ALL: {
                messagingTemplate.convertAndSend(endpoint, JsonHelper.toJson(wsMessage.getMessage()));
            }
            break;
            case USER: {
                SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
                headerAccessor.setSessionId(receiverId.toString());
                headerAccessor.setLeaveMutable(true);
                wsMessage.getMessage().setSenderId(wsMessage.getSenderId());
                messagingTemplate.convertAndSend(endpoint, JsonHelper.toJson(wsMessage.getMessage()), headerAccessor.getMessageHeaders());
            }
            break;
            case GROUP: {
                messagingTemplate.convertAndSend(endpoint, JsonHelper.toJson(wsMessage.getMessage()));
            }
            break;
            default: {
                log.error("<Error>: Destination type not set (used default USER)");
                SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
                headerAccessor.setSessionId(receiverId.toString());
                headerAccessor.setLeaveMutable(true);
                messagingTemplate.convertAndSend(endpoint, JsonHelper.toJson(wsMessage.getMessage()), headerAccessor.getMessageHeaders());
            }
        }

        exchange.getIn().setHeader("endpoint", endpoint);
        exchange.getIn().setHeader(AbstractCamelProcessor.RETURN_HEADER, true);
    }
}
