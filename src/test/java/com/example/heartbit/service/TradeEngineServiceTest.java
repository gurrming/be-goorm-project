package com.example.heartbit.service;

import com.example.heartbit.domain.Category;
import com.example.heartbit.domain.Order;
import com.example.heartbit.domain.OrderStatus;
import com.example.heartbit.domain.OrderType;
import com.example.heartbit.dto.TradeResponse;
import com.example.heartbit.dto.order.OrderBookResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@ActiveProfiles("test")
@SpringBootTest
class TradeEngineServiceTest {

    @Autowired
    private TradeEngineService tradeEngineService;

    @DisplayName("매수 주문 시 호가창에 있는 매도 물량 중 가장 낮은 가격부터 체결된다.")
    @Test
    void processOrderWithPricePriority() {
        // given
        Category category = createCategory();

        Order expensiveSell = createOrder(1L, category, OrderType.SELL, "11000", "1");
        Order cheapSell = createOrder(2L, category, OrderType.SELL, "10000", "1");

        tradeEngineService.processOrder(expensiveSell);
        tradeEngineService.processOrder(cheapSell);

        Order buyOrder = createOrder(3L, category, OrderType.BUY, "12000", "1");

        // when
        List<TradeResponse> results = tradeEngineService.processOrder(buyOrder);

        // then
        assertThat(results).hasSize(1)
                .extracting("tradePrice", "sellOrderId")
                .containsExactly(
                        tuple(new BigDecimal("10000"), 2L)
                );
    }

    @DisplayName("가격이 같으면 먼저 주문한 주문이 먼저 체결된다.")
    @Test
    void processOrderPriority() {
        // given
        Category category = createCategory();

        Order firstSell = createOrder(10L, category, OrderType.SELL, "50000", "1");
        Order secondSell = createOrder(11L, category, OrderType.SELL, "50000", "1");

        tradeEngineService.processOrder(firstSell);
        tradeEngineService.processOrder(secondSell);

        Order buyOrder = createOrder(12L, category, OrderType.BUY, "50000", "1");

        // when
        List<TradeResponse> results = tradeEngineService.processOrder(buyOrder);

        // then
        assertThat(results).hasSize(1)
                .extracting("sellOrderId")
                .containsExactly(10L);
    }

    @DisplayName("주문 수량이 호가창의 대기 물량보다 많으면 호가창의 수량만큼 체결되고 나머지는 호가창에 남는다.")
    @Test
    void processOrderPartial() {
        // given
        Category category = createCategory();
        Order sellOrder = createOrder(1L, category, OrderType.SELL, "50000", "1");
        tradeEngineService.processOrder(sellOrder);

        Order buyOrder = createOrder(1L, category, OrderType.BUY, "50000", "3");

        // when
        List<TradeResponse> results = tradeEngineService.processOrder(buyOrder);

        // then
        assertThat(results).hasSize(1)
                .extracting("tradeCount")
                .containsExactly(new BigDecimal("1"));

        assertThat(buyOrder.getRemainingCount()).isEqualByComparingTo("2");

        List<OrderBookResponse> buyOrderBook = tradeEngineService.getMatchingOrder(category.getCategoryId())
                .getSnapshot(OrderType.BUY, 30);

        assertThat(buyOrderBook).hasSize(1)
                .extracting("orderPrice", "totalRemainingCount")
                .containsExactly(tuple(new BigDecimal("50000"), new BigDecimal("2")));
    }

    private Category createCategory() {
        return Category.builder()
                .categoryId(1L)
                .symbol("BTC")
                .build();
    }

    private Order createOrder(Long orderId, Category category, OrderType type, String price, String count) {
        return Order.builder()
                .orderId(orderId)
                .category(category)
                .orderType(type)
                .orderPrice(new BigDecimal(price))
                .orderCount(new BigDecimal(count))
                .remainingCount(new BigDecimal(count))
                .orderStatus(OrderStatus.OPEN)
                .build();
    }
}