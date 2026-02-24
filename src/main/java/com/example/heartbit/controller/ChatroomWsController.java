package com.example.heartbit.controller;

import com.example.heartbit.dto.ChatRequestDto;
import com.example.heartbit.dto.ChatResponseDto;
import com.example.heartbit.service.ChatroomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatroomWsController {

    private final ChatroomService chatroomService;
    private final RedisTemplate<String, Object> redisTemplate;

    // 채팅 보내기
    // 클라이언트 -> 서버: /app/chat/{categoryId}
    @MessageMapping("/chat/{categoryId}")
    public void send(
            @DestinationVariable Long categoryId,
            IncomingMessage incoming
    ) {
        if (incoming.getMemberId() == null) {
            throw new IllegalArgumentException("memberId는 필수입니다.");
        }
        if (incoming.getChatContent() == null || incoming.getChatContent().isBlank()) {
            throw new IllegalArgumentException("chatContent는 비어있을 수 없습니다.");
        }

        // DB 저장
        ChatRequestDto requestDto = new ChatRequestDto(
                categoryId,
                incoming.getChatContent(),
                incoming.getMemberId()
        );

        ChatResponseDto saved = chatroomService.writeChat(requestDto);

        redisTemplate.convertAndSend("ws-chat-channel", saved);

        // 서버 -> 클라이언트 : /topic/chat/{categoryId}
        // messagingTemplate.convertAndSend("/topic/chat/" + categoryId, saved);
    }

    // 채팅 수신 (DTO)
    public static class IncomingMessage {
        private Long memberId;
        private String chatContent;

        public IncomingMessage() {}

        public Long getMemberId() { return memberId; }
        public void setMemberId(Long memberId) { this.memberId = memberId; }

        public String getChatContent() { return chatContent; }
        public void setChatContent(String chatContent) { this.chatContent = chatContent; }
    }
}
