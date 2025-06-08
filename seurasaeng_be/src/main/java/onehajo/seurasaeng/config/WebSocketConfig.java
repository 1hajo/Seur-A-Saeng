package onehajo.seurasaeng.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    /**
     * WebSocket 연결 Endpoint 등록
     * 클라이언트는 해당 Endpoint를 통해 WebSocket 연결을 초기화.
     *
     * @param registry StompEndpointRegistry
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS();

    }

    /**
     * 메시지 브로커 구성
     * 클라이언트가 메시지를 보내는 경로와 클라이언트가 구독하는 경로를 설정.
     *
     * @param registry MessageBrokerRegistry
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic"); // 클라이언트 구독 주소 prefix
        registry.setApplicationDestinationPrefixes("/app"); // 클라이언트 송신 주소 prefix
    }
}