package com.example.heartbit.service;

import com.example.heartbit.domain.OrderType;
import com.example.heartbit.dto.order.OrderBookResponse;
import com.example.heartbit.engine.core.OrderBook;
import com.example.heartbit.engine.core.OrderBookCategory;
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
    private final OrderBookCategory orderBookContainer;

    public void broadcastOrderBook(Long categoryId) {
        OrderBook book = orderBookContainer.getOrderBook(categoryId);
        broadcastOrderBook(categoryId,
                book.orderBookSnapshot(OrderType.BUY, 30),
                book.orderBookSnapshot(OrderType.SELL, 30));
    }

    public void broadcastOrderBook(Long categoryId, List<OrderBookResponse> buySide, List<OrderBookResponse> sellSide) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("categoryId", categoryId);
        payload.put("buySide", buySide);
        payload.put("sellSide", sellSide);

        messagingTemplate.convertAndSend("/topic/orderbook/" + categoryId, (Object) payload);
    }
}