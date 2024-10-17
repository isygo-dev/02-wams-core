package eu.isygoit.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.messaging.*;

/**
 * The type Ws listener.
 */
@Slf4j
//@Component
public class WsListener {

    //@EventListener(SessionConnectEvent.class)
    private void handleSessionConnected(SessionConnectedEvent event) {
        log.info("SessionConnectedEvent: {}", event.toString());
    }

    //@EventListener(SessionConnectEvent.class)
    private void handleSessionConnected(SessionConnectEvent event) {
        log.info("SessionConnectEvent: {}", event.toString());
    }

    //@EventListener(SessionDisconnectEvent.class)
    private void handleSessionDisconnect(SessionDisconnectEvent event) {
        log.info("SessionDisconnectEvent: {}", event.toString());
    }

    //@EventListener(SessionSubscribeEvent.class)
    private void handleSessionSubscribe(SessionSubscribeEvent event) {
        log.info("SessionSubscribeEvent: {}", event.toString());
    }

    //@EventListener(SessionUnsubscribeEvent.class)
    private void handleSessionUnsubscribe(SessionUnsubscribeEvent event) {
        log.info("SessionUnsubscribeEvent: {}", event.toString());
    }
}
