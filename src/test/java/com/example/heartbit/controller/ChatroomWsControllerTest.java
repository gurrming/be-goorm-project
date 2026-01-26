package com.example.heartbit.controller;

import com.example.heartbit.dto.ChatRequestDto;
import com.example.heartbit.dto.ChatResponseDto;
import com.example.heartbit.service.ChatroomService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatroomWsControllerTest {

    @InjectMocks ChatroomWsController wsController;
    @Mock ChatroomService chatroomService;
    @Mock SimpMessagingTemplate messagingTemplate;

    @Test
    void wsTest() {
        // given
        ChatroomWsController.IncomingMessage msg = new ChatroomWsController.IncomingMessage();
        msg.setMemberId(1L);
        msg.setChatContent("확인용 ");

        given(chatroomService.writeChat(any(ChatRequestDto.class)))
                .willReturn(ChatResponseDto.builder().chatContent("하이").build());

        // when
        wsController.send(1L, msg); // 컨트롤러 메서드 직접 실행

        // then
        verify(chatroomService).writeChat(any()); // DB 저장 호출 확인
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/1"), any(ChatResponseDto.class)); // 전송 호출 확인
    }
}