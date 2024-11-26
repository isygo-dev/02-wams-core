package eu.isygoit.async.camel.processor;

import eu.isygoit.com.camel.processor.AbstractCamelProcessor;
import eu.isygoit.dto.wsocket.WsMessageDto;
import eu.isygoit.dto.wsocket.WsMessageWrapperDto;
import eu.isygoit.dto.wsocket.WsSubscribeDto;
import eu.isygoit.enums.IEnumWSBroker;
import eu.isygoit.enums.IEnumWSEndpoint;
import eu.isygoit.enums.IEnumWSMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * The type Ws chat subscribe processor.
 */
@Slf4j
@Component
@Qualifier("wsChatSubscribeProcessor")
public class WsChatSubscribeProcessor extends AbstractCamelProcessor<WsSubscribeDto> {

    @Override
    public void performProcessor(Exchange exchange, WsSubscribeDto wsSubscribe) throws Exception {
        WsMessageWrapperDto message = WsMessageWrapperDto.builder()
                .endPoint(IEnumWSEndpoint.Types.CHAT)
                .broker(IEnumWSBroker.Types.GROUP)
                .senderId(wsSubscribe.getSenderId())
                .message(WsMessageDto.builder()
                        .type(IEnumWSMessage.Types.STATUS)
                        .senderId(wsSubscribe.getSenderId())
                        .content(wsSubscribe.getStatus().name())
                        .build())
                .build();

        exchange.getIn().setHeader("receiverId", wsSubscribe.getGroupId());
        exchange.getIn().setBody(message);
        exchange.getIn().setHeader(AbstractCamelProcessor.RETURN_HEADER, true);
    }
}
