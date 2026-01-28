package com.example.heartbit.service;

import com.example.heartbit.domain.OrderType;
import com.example.heartbit.dto.order.OrderBookResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderBookService {
    private final TradeEngineService tradeEngineService;
    private final SimpMessagingTemplate messagingTemplate;

    // 호가창 데이터를 전송하는 역할
    public void broadcastOrderBook(Long categoryId) {
        TradeEngineService.MatchingOrder engineOrderBook = tradeEngineService.getMatchingOrder(categoryId);
        if (engineOrderBook == null) return;

        // map을 사용하여 새로운 리스트를 생성
        List<OrderBookResponse> buyOrderBook = engineOrderBook.getSnapshot(OrderType.BUY, 30).stream()
                .map(o -> OrderBookResponse.builder()
                        .orderPrice(formatPrice(o.getOrderPrice()))
                        .totalRemainingCount(o.getTotalRemainingCount())
                        .build())
                .toList();

        List<OrderBookResponse> sellOrderBook = engineOrderBook.getSnapshot(OrderType.SELL, 30).stream()
                .map(o -> OrderBookResponse.builder()
                        .orderPrice(formatPrice(o.getOrderPrice()))
                        .totalRemainingCount(o.getTotalRemainingCount())
                        .build())
                .toList();

        Map<String, Object> payload = Map.of(
                "categoryId", categoryId,
                "buySide", buyOrderBook,
                "sellSide", sellOrderBook,
                "currentPrice", formatPrice(engineOrderBook.getCurrentTradePrice()),
                "serverTime", LocalDateTime.now().toString()
        );
        messagingTemplate.convertAndSend("/topic/orderbook/" + categoryId, (Object) payload);
    }

    // 가격 정규화
    private BigDecimal formatPrice(BigDecimal price) {
        if (price == null) return BigDecimal.ZERO;
        if (price.compareTo(BigDecimal.TEN) < 0) return price.setScale(2, RoundingMode.FLOOR);
        if (price.compareTo(BigDecimal.valueOf(100)) < 0) return price.setScale(1, RoundingMode.FLOOR);
        return price.setScale(0, RoundingMode.FLOOR);
    }
}

