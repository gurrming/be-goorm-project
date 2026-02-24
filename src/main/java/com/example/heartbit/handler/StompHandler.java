package com.example.heartbit.handler;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

    private final MeterRegistry meterRegistry; // 마이크로미터 (프로메테우스 연동용)

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && accessor.getCommand() != null) {
            // 카운터 이름: stomp.messages
            // 태그: type=SEND/CONNECT/SUBSCRIBE 등, direction=IN/OUT
            Counter.builder("stomp.messages")
                    .tag("type", accessor.getCommand().name())
                    .tag("direction", isInbound(channel) ? "IN" : "OUT")
                    .register(meterRegistry)
                    .increment();
        }
        return message;
    }

    // 채널 이름으로 방향 구분 (설정에 따라 이름이 다를 수 있음)
    private boolean isInbound(MessageChannel channel) {
        // 보통 'clientInboundChannel'이 들어오는 것, 'clientOutboundChannel'이 나가는 것
        return channel.toString().contains("Inbound");
    }
}
