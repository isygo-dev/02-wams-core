package eu.isygoit.config;

import eu.isygoit.enums.IEnumWSBroker;
import eu.isygoit.enums.IEnumWSEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * The type Web socket config.
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {


    private final WsChannelInterceptor wsChannelInterceptor;

    @Autowired
    public WebSocketConfig(WsChannelInterceptor wsChannelInterceptor) {
        this.wsChannelInterceptor = wsChannelInterceptor;
    }

    //Can be tested on https://jxy.me/websocket-debug-tool/
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(
                        "/socket/" + IEnumWSEndpoint.Types.NOTIFICATION.name().toLowerCase(),
                        "/socket/" + IEnumWSEndpoint.Types.CHAT.name().toLowerCase(),
                        "/socket/" + IEnumWSEndpoint.Types.VISIO.name().toLowerCase(),
                        "/socket/" + IEnumWSEndpoint.Types.LOGIN.name().toLowerCase())
                .setAllowedOriginPatterns("*");
        registry.addEndpoint(
                        "/socket/" + IEnumWSEndpoint.Types.NOTIFICATION.name().toLowerCase(),
                        "/socket/" + IEnumWSEndpoint.Types.CHAT.name().toLowerCase(),
                        "/socket/" + IEnumWSEndpoint.Types.VISIO.name().toLowerCase(),
                        "/socket/" + IEnumWSEndpoint.Types.LOGIN.name().toLowerCase())
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker(
                /* notification topics */
                "/" + IEnumWSEndpoint.Types.NOTIFICATION.name().toLowerCase() + "/" + IEnumWSBroker.Types.USER.name().toLowerCase(),
                "/" + IEnumWSEndpoint.Types.NOTIFICATION.name().toLowerCase() + "/" + IEnumWSBroker.Types.GROUP.name().toLowerCase(),
                "/" + IEnumWSEndpoint.Types.NOTIFICATION.name().toLowerCase() + "/" + IEnumWSBroker.Types.ALL.name().toLowerCase(),
                /* chat topics */
                "/" + IEnumWSEndpoint.Types.CHAT.name().toLowerCase() + "/" + IEnumWSBroker.Types.USER.name().toLowerCase(),
                "/" + IEnumWSEndpoint.Types.CHAT.name().toLowerCase() + "/" + IEnumWSBroker.Types.GROUP.name().toLowerCase(),
                /* login topics */
                "/" + IEnumWSEndpoint.Types.LOGIN.name().toLowerCase() + "/" + IEnumWSBroker.Types.USER.name().toLowerCase());
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(wsChannelInterceptor);
    }
}
