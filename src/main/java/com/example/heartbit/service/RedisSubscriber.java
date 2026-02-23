package com.example.heartbit.service;

import com.example.heartbit.dto.ChatResponseDto;
import com.example.heartbit.dto.InvestResponse;
import com.example.heartbit.dto.NotificationResponseDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriber {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public void sendChatUpdate(String publishMessage) {
        try {
            ChatResponseDto chatResponse = objectMapper.readValue(publishMessage, ChatResponseDto.class);

            messagingTemplate.convertAndSend("/topic/chat/" + chatResponse.getCategoryId(), chatResponse);
        } catch (Exception e) {
            log.error("채팅 구독 메시지 처리 중 오류 발생: {}", e.getMessage());
        }
    }

    public void sendNotificationUpdate(String publishMessage) {
        try {
            NotificationResponseDto notificationResponse = objectMapper.readValue(publishMessage, NotificationResponseDto.class);

            messagingTemplate.convertAndSend("/topic/notification/" + notificationResponse.getMemberId(), notificationResponse);
        } catch (Exception e) {
            log.error("알림 구독 메시지 처리 중 오류 발생: {}", e.getMessage());
        }
    }

    public void sendTickerUpdate(String publishMessage) {
        try {
            Map<String, Object> ticker = objectMapper.readValue(publishMessage, Map.class);
            Long categoryId = Long.valueOf(ticker.get("categoryId").toString());

            messagingTemplate.convertAndSend("/topic/ticker/" + categoryId, (Object)ticker);
        } catch (Exception e) {
            log.error("Ticker 전송 오류", e);
        }
    }

    public void sendTradesUpdate(String publishMessage) {
        try {
            Map<String, Object> trades = objectMapper.readValue(publishMessage, Map.class);
            Long categoryId = Long.valueOf(trades.get("categoryId").toString());

            messagingTemplate.convertAndSend("/topic/trades/" + categoryId, (Object)trades);
        } catch (Exception e) {
            log.error("Trades 전송 오류", e);
        }
    }

    public void sendChartsUpdate(String publishMessage) {
        try {
            Map<String, Object> charts = objectMapper.readValue(publishMessage, Map.class);
            Long categoryId = Long.valueOf(charts.get("categoryId").toString());

            messagingTemplate.convertAndSend("/topic/charts/" + categoryId, (Object)charts);
        } catch (Exception e) {
            log.error("Charts 전송 오류", e);
        }
    }

    public void sendOrderbookPriceUpdate(String publishMessage) {
        try {
            Map<String, Object> lastPrice = objectMapper.readValue(publishMessage, Map.class);
            Long categoryId = Long.valueOf(lastPrice.get("categoryId").toString());

            messagingTemplate.convertAndSend("/topic/orderbook/lastPrice/" + categoryId, (Object)lastPrice);
        } catch (Exception e) {
            log.error("lastPrice 전송 오류", e);
        }
    }

    public void sendInvestUpdate(String publishMessage) {
        try {
            JsonNode rootNode = objectMapper.readTree(publishMessage);

            Long memberId = rootNode.get("memberId").asLong();
            InvestResponse data = objectMapper.treeToValue(rootNode.get("totalSummary"), InvestResponse.class);

            messagingTemplate.convertAndSend("/topic/invest/" + memberId, data);
        } catch (Exception e) {
            log.error("Invest 웹소켓 전송 오류", e);
        }
    }

    public void sendOrderbookUpdate(String publishMessage) {
        try {
            JsonNode rootNode = objectMapper.readTree(publishMessage);

            Long categoryId = rootNode.get("categoryId").asLong();
            messagingTemplate.convertAndSend("/topic/orderbook/" + categoryId, rootNode);
        } catch (Exception e) {
            log.error("Orderbook 웹소켓 전송 오류", e);
        }
    }

}