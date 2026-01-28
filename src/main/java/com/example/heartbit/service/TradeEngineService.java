package com.example.heartbit.service;

import com.example.heartbit.domain.Order;
import com.example.heartbit.domain.OrderType;
import com.example.heartbit.dto.order.EngineMatchResult;
import com.example.heartbit.dto.TradeResponse;
import com.example.heartbit.dto.order.OrderBookResponse;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class TradeEngineService {
    private final Map<Long, MatchingOrder> machingOrderBooks = new ConcurrentHashMap<>();

    public MatchingOrder getMatchingOrder(Long categoryId) {
        return machingOrderBooks.get(categoryId);
    }

    public BigDecimal normalizePrice(BigDecimal price) {
        if (price == null) return BigDecimal.ZERO;
        if (price.compareTo(new BigDecimal("100")) >= 0) {
            return price.setScale(0, RoundingMode.FLOOR);
        }
        else if (price.compareTo(new BigDecimal("10")) >= 0) {
            return price.setScale(1, RoundingMode.FLOOR);
        }
        else {
            return price.setScale(2, RoundingMode.FLOOR);
        }
    }

    public synchronized List<TradeResponse> processOrder(Order newOrder) {
        newOrder.setOrderPrice(normalizePrice(newOrder.getOrderPrice()));

        Long categoryId = newOrder.getCategory().getCategoryId();
        MatchingOrder orderBook = machingOrderBooks.computeIfAbsent(categoryId, id -> new MatchingOrder());

        // 새 주문이 들어오면 기존 호가창 대기 물량과 비교
        List<EngineMatchResult> engineResults = orderBook.match(newOrder);

        // 매칭 후 매도/매수 수량이 남았다면 호가창에 등록
        if (newOrder.getRemainingCount().compareTo(BigDecimal.ZERO) > 0) {
            orderBook.addOrderBook(newOrder);
        }

        // takerType을 주문한 사람 타입으로 고정하여 반환
        return engineResults.stream()
            .map(result -> {
                // Taker(새주문)가 BUY면 Maker는 SELL
                boolean isTakerBuy = result.getTaker().getOrderType() == OrderType.BUY;

                return TradeResponse.builder()
                        .buyOrderId(isTakerBuy ? result.getTaker().getOrderId() : result.getMaker().getOrderId())
                        .sellOrderId(isTakerBuy ? result.getMaker().getOrderId() : result.getTaker().getOrderId())
                        .buyOrderEntity(isTakerBuy ? result.getTaker() : result.getMaker())
                        .sellOrderEntity(isTakerBuy ? result.getMaker() : result.getTaker())
                        .tradePrice(result.getTradePrice())
                        .tradeCount(result.getTradeCount())
                        .tradeTime(result.getTradeTime())
                        .takerType(newOrder.getOrderType().name())
                        .build();
                })
                .toList();
    }

    public static class MatchingOrder {
        // 매수 매도 값
        private final Map<BigDecimal, PriorityQueue<Order>> buyOrderBook = new HashMap<>();
        private final Map<BigDecimal, PriorityQueue<Order>> sellOrderBook = new HashMap<>();
        // 가격들마다의 수량의 순서를 관리
        private final PriorityQueue<BigDecimal> buyPrices = new PriorityQueue<>(Comparator.reverseOrder());
        private final PriorityQueue<BigDecimal> sellPrices = new PriorityQueue<>();
        // 가격 조회
        private final Set<BigDecimal> isBuyPrices = new HashSet<>();
        private final Set<BigDecimal> isSellPrices = new HashSet<>();

        @Getter
        private BigDecimal currentTradePrice = BigDecimal.ZERO;

        // 현재가 기준으로 가져오기 호가창 가져오기
        public List<OrderBookResponse> getSnapshot(OrderType type, int limit) {
            PriorityQueue<BigDecimal> prices = (type == OrderType.BUY) ? buyPrices : sellPrices;
            Map<BigDecimal, PriorityQueue<Order>> orderBook = (type == OrderType.BUY) ? buyOrderBook : sellOrderBook;

            Comparator<BigDecimal> priceComparator = (type == OrderType.BUY)
                    ? Comparator.reverseOrder()
                    : Comparator.naturalOrder();

            return prices.stream()
                    .sorted(priceComparator)
                    .limit(limit)
                    .map(price -> OrderBookResponse.builder()
                            .orderPrice(price)
                            .totalRemainingCount(orderBook.get(price).stream()
                                    .map(Order::getRemainingCount)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add))
                            .build())
                    .toList();
        }

        // 주문된 가격이 있는지 확인 후 호가창에 추가
        private void addOrderBook(Order order) {
            BigDecimal price = order.getOrderPrice();
            if (order.getOrderType() == OrderType.BUY) {
                // 매수 주문
                buyOrderBook.computeIfAbsent(price, key -> new PriorityQueue<>(Comparator.comparing(Order::getOrderId)))
                        .add(order);
                // buyPrices에 추가
                if (isBuyPrices.add(price)) {
                    buyPrices.add(price);
                }
            } else {
                // 매도 주문
                sellOrderBook.computeIfAbsent(price, key -> new PriorityQueue<>(Comparator.comparing(Order::getOrderId)))
                        .add(order);
                if (isSellPrices.add(price)) {
                    sellPrices.add(price);
                }
            }
        }
        /// TODO:
        /// 1. 여러개의 종목을 매칭하는것으로 바꿔보자
        /// 2. 테스트를 충분히해보자
        /// 3. Order 부분 확인 및 uid도 확인 필요!(수정될거같음)

        // 매칭
        public List<EngineMatchResult> match(Order newOrder) {
            List<EngineMatchResult> tradeList = new ArrayList<>();
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
                Order taker, PriorityQueue<Order> makerOrders, PriorityQueue<BigDecimal> priceQueue,
                List<EngineMatchResult> tradeList, BigDecimal remaining) {

            while (remaining.compareTo(BigDecimal.ZERO) > 0 && !makerOrders.isEmpty()) {
                Order maker = makerOrders.peek();
                BigDecimal tradeCount = remaining.min(maker.getRemainingCount());

                this.currentTradePrice = maker.getOrderPrice();

                // 체결 리스트 가져오기
                tradeList.add(new EngineMatchResult(
                        taker,
                        maker,
                        this.currentTradePrice,
                        tradeCount,
                        LocalDateTime.now()
                ));
                // 수량 등록
                maker.updateRemainingCount(tradeCount);
                taker.updateRemainingCount(tradeCount);
                remaining = taker.getRemainingCount();
                // 수량이 0이 되면 제거
                if (maker.getRemainingCount().compareTo(BigDecimal.ZERO) == 0) makerOrders.poll();
            }
            //
            if (makerOrders.isEmpty()) {
                // 큐에서 제거
                BigDecimal zeroPrice = priceQueue.poll();
                // HashSet에서도 제거
                if (taker.getOrderType() == OrderType.BUY) {
                    // Taker가 매수면 매도 호가창의 가격 사라짐
                    isSellPrices.remove(zeroPrice);
                    sellOrderBook.remove(zeroPrice);
                } else {
                    // Taker가 매도면 매수 호가창의 가격 사라짐
                    isBuyPrices.remove(zeroPrice);
                    buyOrderBook.remove(zeroPrice);
                }
            }
            return remaining;
        }
    }

    public void deleteAllOrders() {
        machingOrderBooks.clear();
    }

}