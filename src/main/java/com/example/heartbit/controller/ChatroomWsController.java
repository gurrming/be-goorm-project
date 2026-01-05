package com.example.heartbit.controller;

import com.example.heartbit.dto.ChatroomRequestDto;
import com.example.heartbit.dto.ChatroomResponseDto;
import com.example.heartbit.service.ChatroomService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatroomWsController {

    private final ChatroomService chatroomService;
    private final SimpMessagingTemplate messagingTemplate;

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
        if (incoming.getChatroomContent() == null || incoming.getChatroomContent().isBlank()) {
            throw new IllegalArgumentException("chatroomContent는 비어있을 수 없습니다.");
        }

        // DB 저장
        ChatroomRequestDto requestDto = new ChatroomRequestDto(
                categoryId,
                incoming.getChatroomContent(),
                incoming.getMemberId()
        );

        ChatroomResponseDto saved = chatroomService.writeChat(requestDto);

        // 서버 -> 클라이언트 브로드캐스트: /topic/chat/{categoryId}
        messagingTemplate.convertAndSend("/topic/chat/" + categoryId, saved);
    }

    // 채팅 수신 (DTO로)
    public static class IncomingMessage {
        private Long memberId;
        private String chatroomContent;

        public IncomingMessage() {}

        public Long getMemberId() { return memberId; }
        public void setMemberId(Long memberId) { this.memberId = memberId; }

        public String getChatroomContent() { return chatroomContent; }
        public void setChatroomContent(String chatroomContent) { this.chatroomContent = chatroomContent; }
    }
}
