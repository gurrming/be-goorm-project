package com.example.heartbit.service;

import com.example.heartbit.dto.order.OrderBookResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderBookService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastOrderBook(Long categoryId, List<OrderBookResponse> buySide, List<OrderBookResponse> sellSide) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("categoryId", categoryId);
        payload.put("buySide", buySide);
        payload.put("sellSide", sellSide);

        messagingTemplate.convertAndSend("/topic/orderbook/" + categoryId, (Object) payload);
    }
}