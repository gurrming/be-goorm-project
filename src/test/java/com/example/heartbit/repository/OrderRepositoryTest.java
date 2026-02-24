package com.example.heartbit.repository;

import com.example.heartbit.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;


@ActiveProfiles("test")
@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @DisplayName("특정 회원의 주문 내역을 최근 주문 순으로 조회한다.")
    @Test
    void findByMember_MemberIdOrderByOrderTimeDesc() {
        // given
        Member member = memberRepository.save(Member.builder()
                .memberEmail("test@test.com")
                .memberNickname("goorming")
                .memberPassword("0000")
                .build());
        Category category = categoryRepository.save(Category.builder().symbol("BTC").build());

        Order order1 = createOrder(member, category, OrderStatus.OPEN, OrderType.BUY);
        Order order2 = createOrder(member, category, OrderStatus.OPEN, OrderType.SELL);
        orderRepository.saveAll(List.of(order1, order2));

        // when
        List<Order> result = orderRepository.findByMember_MemberIdOrderByOrderTimeDesc(member.getMemberId());

        // then
        assertThat(result).hasSize(2)
                .extracting("orderId")
                .containsExactly(order2.getOrderId(), order1.getOrderId());
    }

    @DisplayName("특정 회원의 미체결(OPEN, PARTIAL) 주문 내역을 최근 주문 순으로 조회한다.")
    @Test
    void findByMember_MemberIdAndOrderStatusInOrderByOrderTimeDesc() {
        // given
        Member member = memberRepository.save(Member.builder()
                .memberEmail("test@test.com")
                .memberNickname("유진")
                .memberPassword("0000")
                .build());
        Category category = categoryRepository.save(Category.builder().symbol("BTC").build());
        List<OrderStatus> activeStatuses = List.of(OrderStatus.OPEN, OrderStatus.PARTIAL);

        Order order1 = createOrder(member, category, OrderStatus.OPEN, OrderType.BUY);
        Order order2 = createOrder(member, category, OrderStatus.OPEN, OrderType.SELL);
        Order order3 = createOrder(member, category, OrderStatus.PARTIAL, OrderType.SELL);
        Order order4 = createOrder(member, category, OrderStatus.FILLED, OrderType.BUY);
        orderRepository.saveAll(List.of(order1, order2, order3, order4));

        // when
        Slice<Order> result = orderRepository.findByMember_MemberIdAndOrderStatusInOrderByOrderTimeDesc(member.getMemberId(), activeStatuses, Pageable.unpaged());

        // then
        assertThat(result).hasSize(3)
                .extracting("orderId")
                .containsExactly(order3.getOrderId(), order2.getOrderId(), order1.getOrderId());
    }

    @DisplayName("회원의 미체결(OPEN, PARTIAL) 주문 수량 총합을 조회한다.")
    @Test
    void countOpenOrdersByMember() {
        // given
        Member member = memberRepository.save(Member.builder()
                .memberEmail("test@test.com")
                .memberNickname("유진")
                .memberPassword("0000")
                .build());
        Category category = categoryRepository.save(Category.builder().symbol("BTC").build());
        List<OrderStatus> activeStatuses = List.of(OrderStatus.OPEN, OrderStatus.PARTIAL);

        orderRepository.save(createOrder(member, category, OrderStatus.OPEN, OrderType.BUY));
        orderRepository.save(createOrder(member, category, OrderStatus.PARTIAL, OrderType.BUY));
        orderRepository.save(createOrder(member, category, OrderStatus.PARTIAL, OrderType.BUY));
        orderRepository.save(createOrder(member, category,OrderStatus.FILLED, OrderType.BUY));

        // when
        Long count = orderRepository.countOpenOrdersByMember(member.getMemberId(), activeStatuses);

        // then
        assertThat(count).isEqualTo(3L);
    }

    @DisplayName("서버가 다시 켜졌을 때 이전 순서 그대로 조회한다.")
    @Test
    void findByOrderStatusInOrderByOrderTimeAsc() {
        // given
        Category category = categoryRepository.save(Category.builder().symbol("BTC").build());
        List<OrderStatus> activeStatuses = List.of(OrderStatus.OPEN, OrderStatus.PARTIAL);

        Order order1 = createOrder(null, category, OrderStatus.OPEN, OrderType.BUY);
        Order order2 = createOrder(null, category, OrderStatus.PARTIAL, OrderType.SELL);
        Order order3 = createOrder(null, category, OrderStatus.FILLED, OrderType.SELL);

        orderRepository.saveAll(List.of(order1, order2, order3));

        // when
        List<Order> result = orderRepository.findByOrderStatusInOrderByOrderTimeAsc(activeStatuses);

        // then
        assertThat(result).hasSize(2)
                .extracting("orderId")
                .containsExactly(order1.getOrderId(), order2.getOrderId());
    }

    private Order createOrder(Member member, Category category, OrderStatus status, OrderType type) {
        return Order.builder()
                .member(member)
                .category(category)
                .orderStatus(status)
                .orderType(type)
                .orderPrice(new BigDecimal("10000"))
                .orderCount(new BigDecimal("1"))
                .remainingCount(new BigDecimal("1"))
                .build();
    }
}
