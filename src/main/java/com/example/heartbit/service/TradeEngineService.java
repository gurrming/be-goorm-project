package com.example.heartbit.service;

import com.example.heartbit.domain.Order;
import com.example.heartbit.domain.OrderType;
import com.example.heartbit.domain.Trade;
import com.example.heartbit.dto.TradeRequest;
import com.example.heartbit.dto.TradeResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class TradeEngineService {

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

    // 매칭
    public List<Trade> match(Order newOrder) {

        List<Trade> tradeList = new ArrayList<>();


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

                // Trade 객체 생성
                Trade trade = Trade.builder()
                        .buyOrder(buy)
                        .sellOrder(sell)
                        .tradePrice(tradePrice)
                        .tradeCount(tradeCount)
                        .tradeTime(LocalDateTime.now())
                        .takerType(takerType)
                        .build();
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

    // 주문 값 dto로 반환
    public List<TradeResponse> processOrder(Order newOrder) {
        // 호가창에 새로운 주문 등록
        addOrderToBook(newOrder);
        // 매칭 된 값
        List<Trade> trades = match(newOrder);

        boolean lastIsBuyTaker = (newOrder.getOrderType() == OrderType.BUY);

        if (!trades.isEmpty()) {
            // 마지막 체결
            Trade lastTrade = trades.get(trades.size() - 1);
            // 더 늦게 생성된 주문이 Taker
            lastIsBuyTaker = lastTrade.getBuyOrder().getOrderTime().isAfter(lastTrade.getSellOrder().getOrderTime());
        }
        final String finalIsTaker = lastIsBuyTaker ? "BUY" : "SELL";
        // dto로 반환
        return trades.stream()
                .map(trade -> TradeResponse.builder()
                        .buyOrderId(trade.getBuyOrder().getOrderId())
                        .sellOrderId(trade.getSellOrder().getOrderId())
                        .tradePrice(trade.getTradePrice())
                        .tradeCount(trade.getTradeCount())
                        .tradeTime(trade.getTradeTime())
                        .tradeClosePrice(trade.getTradeClosePrice())
                        .takerType(finalIsTaker)
                        .build())
                .toList();
    }
}
