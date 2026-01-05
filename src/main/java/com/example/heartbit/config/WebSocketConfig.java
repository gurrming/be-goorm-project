package com.example.heartbit.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {



    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 채널 구독으로 "/topic" , 서버 -> 클라이언트
        config.enableSimpleBroker("/topic");
        // 클라이언트 -> 서버
        config.setApplicationDestinationPrefixes("/app");

    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        //프론트엔드에서 소켓연결을 시도할 주소 "/ws-heartbit"
        registry.addEndpoint("/ws-heartbit")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

}
