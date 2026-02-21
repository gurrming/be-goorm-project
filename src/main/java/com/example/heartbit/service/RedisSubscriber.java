package com.example.heartbit.service;

import com.example.heartbit.dto.ChatResponseDto;
import com.example.heartbit.dto.NotificationResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriber {

//    private final ObjectMapper objectMapper;
//    private final SimpMessagingTemplate messagingTemplate;
//
//    /**
//     * Redis에서 채팅 메시지가 Publish되면 호출됨
//     */
//    public void sendMessage(String publishMessage) {
//        try {
//            // Redis 발행 메시지를 ChatResponseDto로 변환
//            ChatResponseDto chatResponse = objectMapper.readValue(publishMessage, ChatResponseDto.class);
//            // 해당 카테고리 채팅방을 구독 중인 클라이언트들에게 전송
//            messagingTemplate.convertAndSend("/topic/chat/" + chatResponse.getCategoryId(), chatResponse);
//        } catch (Exception e) {
//            log.error("채팅 구독 처리 중 오류 발생: {}", e.getMessage());
//        }
//    }
//
//    /**
//     * Redis에서 알림 메시지가 Publish되면 호출됨
//     */
//    public void sendNotification(String publishMessage) {
//        try {
//            // Redis 발행 메시지를 NotificationResponseDto로 변환
//            NotificationResponseDto notificationResponse = objectMapper.readValue(publishMessage, NotificationResponseDto.class);
//            // 특정 사용자의 개인 알림 채널로 전송
//            messagingTemplate.convertAndSend("/topic/notification/" + notificationResponse.getMemberId(), notificationResponse);
//        } catch (Exception e) {
//            log.error("알림 구독 처리 중 오류 발생: {}", e.getMessage());
//        }
//    }
}