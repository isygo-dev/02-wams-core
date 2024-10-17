package eu.isygoit.async.camel.processor;

import eu.isygoit.com.camel.processor.AbstractCamelProcessor;
import eu.isygoit.dto.wsocket.WsConnectDto;
import eu.isygoit.dto.wsocket.WsMessageDto;
import eu.isygoit.dto.wsocket.WsMessageWrapperDto;
import eu.isygoit.enums.IEnumWSBroker;
import eu.isygoit.enums.IEnumWSEndpoint;
import eu.isygoit.enums.IEnumWSMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * The type Ws chat connect processor.
 */
@Slf4j
@Component
@Qualifier("wsChatConnectProcessor")
public class WsChatConnectProcessor extends AbstractCamelProcessor<WsConnectDto> {

    @Override
    public void performProcessor(Exchange exchange, WsConnectDto wsConnect) throws Exception {
        WsMessageWrapperDto message = WsMessageWrapperDto.builder()
                .endPoint(IEnumWSEndpoint.Types.CHAT)
                .broker(IEnumWSBroker.Types.GROUP)
                .senderId(wsConnect.getSenderId())
                .message(WsMessageDto.builder()
                        .type(IEnumWSMessage.Types.STATUS)
                        .senderId(wsConnect.getSenderId())
                        .content(wsConnect.getStatus().name())
                        .build())
                .build();

        exchange.getIn().setHeader("receiverId", wsConnect.getGroupId());
        exchange.getIn().setBody(message);
        exchange.getIn().setHeader(AbstractCamelProcessor.RETURN_HEADER, true);
    }
}
