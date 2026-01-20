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
    private final Map<Long, MatchingOrder> machingOrderBooks = new ConcurrentHashMap<>();

    public synchronized List<TradeResponse> processOrder(Order newOrder) {
        Long categoryId = newOrder.getCategory().getCategoryId();
        MatchingOrder orderBook = machingOrderBooks.computeIfAbsent(categoryId, id -> new MatchingOrder());

        // 새 주문이 들어오면 기존 호가창 대기 물량과 비교
        List<TradeRequest> tradeRequests = orderBook.match(newOrder);

        // 매칭 후 매도/매수 수량이 남았다면 호가창에 등록
        if (newOrder.getRemainingCount().compareTo(BigDecimal.ZERO) > 0) {
            orderBook.addOrderToBook(newOrder);
        }

        // takerType을 주문한 사람 타입으로 고정하여 반환
        return tradeRequests.stream()
                .map(request -> TradeResponse.builder()
                        .buyOrderId(request.getBuyOrderId())
                        .sellOrderId(request.getSellOrderId())
                        .tradePrice(request.getTradePrice())
                        .tradeCount(request.getTradeCount())
                        .tradeTime(request.getTradeTime())
                        .takerType(newOrder.getOrderType().name())
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
        /// 1. 여러개의 종목을 매칭하는것으로 바꿔보자
        /// 2. 테스트를 충분히해보자
        /// 3. Order 부분 확인 및 uid도 확인 필요!(수정될거같음)

        // 매칭
        public List<TradeRequest> match(Order newOrder) {
            List<TradeRequest> tradeList = new ArrayList<>();
            BigDecimal remaining = newOrder.getRemainingCount();

            if (newOrder.getOrderType() == OrderType.BUY) {
                // 매도 호가창 가장 싼 가격부터 확인
                while (remaining.compareTo(BigDecimal.ZERO) > 0 && !sellPrices.isEmpty()) {
                    BigDecimal bestSellPrice = sellPrices.peek();
                    // 매수가와 비교 후 더 비싸면 멈춤
                    if (newOrder.getOrderPrice().compareTo(bestSellPrice) < 0) break;
                    remaining = executeTrade(newOrder, sellOrderBook.get(bestSellPrice), sellPrices, tradeList, remaining);
                }
            } else {
                // 매수 호가창 가장 비싼 가격부터 확인
                while (remaining.compareTo(BigDecimal.ZERO) > 0 && !buyPrices.isEmpty()) {
                    BigDecimal bestBuyPrice = buyPrices.peek();
                    // 매수가와 비교 후 더 싸면 멈춤
                    if (newOrder.getOrderPrice().compareTo(bestBuyPrice) > 0) break;
                    remaining = executeTrade(newOrder, buyOrderBook.get(bestBuyPrice), buyPrices, tradeList, remaining);
                }
            }
            return tradeList;
        }

        // 수량 차감
        private BigDecimal executeTrade(
                Order taker, PriorityQueue<Order> makerOrders, PriorityQueue<BigDecimal> priceQueue, List<TradeRequest> tradeList, BigDecimal remaining) {
            while (remaining.compareTo(BigDecimal.ZERO) > 0 && !makerOrders.isEmpty()) {
                Order maker = makerOrders.peek();
                BigDecimal tradeCount = remaining.min(maker.getRemainingCount());

                tradeList.add(new TradeRequest(maker.getOrderPrice(), tradeCount, taker.getCategory().getCategoryId(),
                        taker.getOrderType() == OrderType.BUY ? taker.getOrderId() : maker.getOrderId(),
                        taker.getOrderType() == OrderType.SELL ? taker.getOrderId() : maker.getOrderId(), LocalDateTime.now()));

                maker.updateRemainingCount(tradeCount);
                taker.updateRemainingCount(tradeCount);
                remaining = taker.getRemainingCount();

                if (maker.getRemainingCount().compareTo(BigDecimal.ZERO) == 0) makerOrders.poll();
            }
            if (makerOrders.isEmpty()) {
                priceQueue.poll();
            }
            return remaining;
        }
    }}