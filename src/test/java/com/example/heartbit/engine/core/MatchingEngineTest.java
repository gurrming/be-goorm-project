package com.example.heartbit.engine.core;

import com.example.heartbit.domain.OrderType;
import com.example.heartbit.engine.model.MatchResult;
import com.example.heartbit.engine.model.OrderCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class MatchingEngineTest {

    private MatchingEngine engine = new MatchingEngine();
    private OrderBook book;

    @BeforeEach
    void setUp() {
        book = new OrderBook();
    }

    @Test
    @DisplayName("가격이 일치하는 매수/매도 주문이 들어오면 매칭되어야 한다.")
    void matchSuccessTest() {
        // given
        OrderCommand maker = OrderCommand.builder()
                .orderId(1L).type(OrderType.SELL)
                .orderPrice(new BigDecimal("10000"))
                .remainingCount(new BigDecimal("5")).build();
        book.add(maker);

        // when
        OrderCommand taker = OrderCommand.builder()
                .orderId(2L).type(OrderType.BUY)
                .orderPrice(new BigDecimal("10000"))
                .remainingCount(new BigDecimal("3")).build();

        List<MatchResult> results = engine.match(book, taker);

        // then
        assertThat(results).hasSize(1);
        MatchResult result = results.get(0);
        assertThat(result.getOrderCount()).isEqualTo(new BigDecimal("3"));
        assertThat(result.getPrice()).isEqualTo(new BigDecimal("10000"));

        assertThat(maker.getRemainingCount()).isEqualTo(new BigDecimal("2"));
        assertThat(taker.getRemainingCount().signum()).isEqualTo(0);
    }

    @Test
    @DisplayName("매도 호가가 여러 개일 때 가장 낮은 가격부터 체결된다")
    void pricePriorityTest() {
        // given
        book.add(createOrder(1L, OrderType.SELL, "9000", "1"));
        book.add(createOrder(2L, OrderType.SELL, "10000", "1"));

        // when
        OrderCommand taker = createOrder(3L, OrderType.BUY, "11000", "2");
        List<MatchResult> results = engine.match(book, taker);

        // then
        assertThat(results.get(0).getPrice()).isEqualTo(new BigDecimal("9000"));
        assertThat(results.get(1).getPrice()).isEqualTo(new BigDecimal("10000"));
    }

    private OrderCommand createOrder(Long id, OrderType type, String price, String count) {
        return OrderCommand.builder()
                .orderId(id).type(type)
                .orderPrice(new BigDecimal(price))
                .remainingCount(new BigDecimal(count)).build();
    }
}