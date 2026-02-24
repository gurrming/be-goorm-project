package com.example.heartbit.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class OrderTest {

    @DisplayName("신규 주문 생성 시 남은 수량은 주문 수량과 동일해야 하며 상태는 미체결(OPEN)이다.")
    @Test
    void createOrder() {
        // given
        BigDecimal price = new BigDecimal("50000");
        BigDecimal count = new BigDecimal("1");

        // when
        Order order = Order.builder()
                .orderPrice(price)
                .orderCount(count)
                .orderType(OrderType.BUY)
                .build();

        // then
        assertThat(order.getRemainingCount()).isEqualByComparingTo(count);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.OPEN);
    }

    @DisplayName("일부분만 체결되면 남은 수량이 줄어들고 상태는 부분체결(PARTIAL)이 된다.")
    @Test
    void updateRemainingCountPartial() {
        // given
        Order order = createOrder("10"); // 10개 주문

        // when
        order.updateRemainingCount(new BigDecimal("4"));

        // then
        assertThat(order.getRemainingCount()).isEqualByComparingTo("6");
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PARTIAL);
    }

    @DisplayName("전체 수량이 체결되면 남은 수량은 0이 되고 상태는 체결(FILLED)이 된다.")
    @Test
    void updateRemainingCountFilled() {
        // given
        Order order = createOrder("10");

        // when
        order.updateRemainingCount(new BigDecimal("10"));

        // then
        assertThat(order.getRemainingCount()).isEqualByComparingTo("0");
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.FILLED);
    }

    @DisplayName("체결 수량이 남은 수량보다 크더라도 남은 수량은 0 이하가 되지않는다.")
    @Test
    void updateRemainingCountOver() {
        // given
        Order order = createOrder("10");

        // when
        order.updateRemainingCount(new BigDecimal("15"));

        // then
        assertThat(order.getRemainingCount()).isEqualByComparingTo("0");
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.FILLED);
    }


    @DisplayName("대기 중인 주문은 취소할 수 있으며 상태는 취소(CANCELLED)가 된다.")
    @Test
    void cancelSuccess() {
        // given
        Order order = createOrder("10");

        // when
        order.cancel();

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @DisplayName("이미 전량 체결된 주문은 취소할 수 없으며 예외가 발생한다.")
    @Test
    void cancelFail() {
        // given
        Order order = createOrder("10");
        order.updateRemainingCount(new BigDecimal("10"));

        // when & then
        assertThatThrownBy(order::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 체결된 주문은 취소할 수 없습니다.");
    }

    private Order createOrder(String count) {
        return Order.builder()
                .orderPrice(new BigDecimal("50000"))
                .orderCount(new BigDecimal(count))
                .orderType(OrderType.BUY)
                .build();
    }

}