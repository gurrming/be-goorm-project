package com.example.heartbit.service;

import com.example.heartbit.domain.Order;
import com.example.heartbit.domain.OrderType;
import com.example.heartbit.dto.TradeRequest;
import com.example.heartbit.dto.TradeResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TradeEngineService {
    // 주문 종목별 호가창
    private final Map<Long, MatchingOrder> machingOrderBooks = new ConcurrentHashMap<>();
    // 주문 값 dto로 반환
    public synchronized List<TradeResponse> processOrder(Order newOrder) {
        // 종목 id 값
        Long categoryId = newOrder.getCategory().getCategoryId();

        MatchingOrder orderBook = machingOrderBooks.computeIfAbsent(categoryId, id -> new MatchingOrder());
        // 새 주문
        orderBook.addOrderToBook(newOrder);

        // 매칭 된 값을 List로
        List<TradeRequest> tradeRequests = orderBook.match(newOrder);

        // dto로 반환
        return tradeRequests.stream()
                .map(request -> TradeResponse.builder()
                        .buyOrderId(request.getBuyOrderId())
                        .sellOrderId(request.getSellOrderId())
                        .tradePrice(request.getTradePrice())
                        .tradeCount(request.getTradeCount())
                        .tradeTime(request.getTradeTime())
                        .build())
                .toList();
    }

    private static class MatchingOrder {
        // 매수 매도 값
        private final Map<BigDecimal, PriorityQueue<Order>> buyOrderBook = new HashMap<>();
        private final Map<BigDecimal, PriorityQueue<Order>> sellOrderBook = new HashMap<>();
        // 가격들마다의 수량의 순서를 관리
        private final PriorityQueue<BigDecimal> buyPrices = new PriorityQueue<>(Comparator.reverseOrder());
        private final PriorityQueue<BigDecimal> sellPrices = new PriorityQueue<>();

        // 주문된 가격이 있는지 확인 후 호가창에 추가
        private void addOrderToBook(Order order) {
            BigDecimal price = order.getOrderPrice();
            if (order.getOrderType() == OrderType.BUY) {
                buyOrderBook.computeIfAbsent(price, key -> new PriorityQueue<>(Comparator.comparing(Order::getOrderId)))
                        .add(order);
                if (!buyPrices.contains(price)) buyPrices.add(price);
            } else {
                sellOrderBook.computeIfAbsent(price, key -> new PriorityQueue<>(Comparator.comparing(Order::getOrderId)))
                        .add(order);
                if (!sellPrices.contains(price)) sellPrices.add(price);
            }
        }

        /// TODO:
        ///     1. 여러개의 종목을 매칭하는것으로 바꿔보자
        ///     2. 테스트를 충분히해보자
        ///     3. Order 부분 확인 및 uid도 확인 필요!(수정될거같음)
        // 매칭
        public List<TradeRequest> match(Order newOrder) {

            Long categoryId = newOrder.getCategory().getCategoryId();

            List<TradeRequest> tradeList = new ArrayList<>();

            while (!buyOrderBook.isEmpty() && !sellOrderBook.isEmpty()) {
                BigDecimal nowBuyPrice = buyPrices.peek();
                BigDecimal nowSellPrice = sellPrices.peek();

                // 매수가 더 높은 경우에도 체결
                if (nowBuyPrice.compareTo(nowSellPrice) >= 0) {
                    // 해당하는 주문의 큐
                    PriorityQueue<Order> buyOrders = buyOrderBook.get(nowBuyPrice);
                    PriorityQueue<Order> sellOrders = sellOrderBook.get(nowSellPrice);

                    Order buy = buyOrders.peek();
                    Order sell = sellOrders.peek();

                    BigDecimal tradeCount = buy.getRemainingCount().min(sell.getRemainingCount());
                    BigDecimal tradePrice = sell.getOrderPrice();
                    LocalDateTime tradeTime = LocalDateTime.now();
                    // Trade 객체 생성
                    TradeRequest trade = new TradeRequest(
                            tradePrice,
                            tradeCount,
                            categoryId,
                            buy.getOrderId(),
                            sell.getOrderId(),
                            tradeTime
                    );
                    tradeList.add(trade);

                    buy.updateRemainingCount(tradeCount);
                    sell.updateRemainingCount(tradeCount);

                    // 수량이 0이 된 주문은 제거
                    if (buy.getRemainingCount().compareTo(BigDecimal.ZERO) == 0) buyOrders.poll();
                    if (sell.getRemainingCount().compareTo(BigDecimal.ZERO) == 0) sellOrders.poll();

                    // 해당 가격에서도 제거
                    if (buyOrders.isEmpty()) {
                        buyOrderBook.remove(nowBuyPrice);
                        buyPrices.poll();
                    }
                    if (sellOrders.isEmpty()) {
                        sellOrderBook.remove(nowSellPrice);
                        sellPrices.poll();
                    }
                } else {
                    break;
                }
            }
            return tradeList;
        }
    }
}