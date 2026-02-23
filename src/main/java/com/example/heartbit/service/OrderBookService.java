package com.example.heartbit.service;

import com.example.heartbit.dto.order.OrderBookResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderBookService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void broadcastOrderBook(Long categoryId, List<OrderBookResponse> buySide, List<OrderBookResponse> sellSide) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("categoryId", categoryId);
            payload.put("buySide", buySide);
            payload.put("sellSide", sellSide);

            String jsonPayload = objectMapper.writeValueAsString(payload);

            stringRedisTemplate.convertAndSend("ws-orderbook-channel", jsonPayload);
        } catch (Exception e) {
            log.error("Orderbook Publish Error", e);
        }
    }

}