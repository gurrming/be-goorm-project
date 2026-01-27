package com.example.heartbit.service;

import com.example.heartbit.domain.*;
import com.example.heartbit.dto.order.MemberOpenOrderResponse;
import com.example.heartbit.dto.order.OrderRequest;
import com.example.heartbit.dto.order.OrderResponse;
import com.example.heartbit.repository.*;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class OrderServiceTest {

    @Autowired
    private BotsRepository botsRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private AssetRepository assetRepository;
    @Autowired
    private OrderService orderService;

    @MockitoBean
    private AssetService assetService;

    @AfterEach
    void tearDown() {
        orderRepository.deleteAllInBatch();
        assetRepository.deleteAllInBatch();
        botsRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
    }

    @DisplayName("특정 종목을 봇 ID로 주문하면 자산 차감 없이 주문이 생성된다.")
    @Test
    void createBotOrderOnlyWithBotId() {
        // given
        Bots bot = botsRepository.save(Bots.builder()
                .build());

        Category category = categoryRepository.save(Category.builder()
                .symbol("BTC")
                .build());
        OrderRequest request = OrderRequest.builder()
                .botId(bot.getBotId())
                .categoryId(category.getCategoryId())
                .orderPrice(new BigDecimal("50000"))
                .orderCount(new BigDecimal("1"))
                .orderType(OrderType.BUY)
                .build();
        // when
        orderService.createOrder(request);

        // then
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(1)
                .extracting("orderPrice", "orderCount", "orderType")
                .containsExactly(
                        tuple(new BigDecimal("50000"), new BigDecimal("1"), OrderType.BUY)
                );

        verify(assetService, never()).deductCash(anyLong(), any(BigDecimal.class));

    }

    @DisplayName("사용자가 특정 종목에 대한 주문을 생성한다.")
    @Test
    void createOrder() {
        // given
        Member member = memberRepository.save(Member.builder()
                .memberEmail("test@test.com")
                .memberNickname("유진")
                .memberPassword("1234")
                .build());
        assetRepository.save(Asset.builder()
                .member(member)
                .assetCash(new BigDecimal("200000"))
                .build());
        Category category = categoryRepository.save(Category.builder()
                .symbol("BTC")
                .build());

        BigDecimal orderPrice = new BigDecimal("50000");
        BigDecimal orderCount = new BigDecimal("2");
        BigDecimal totalAmount = orderPrice.multiply(orderCount);

        OrderRequest request = OrderRequest.builder()
                .memberId(member.getMemberId())
                .categoryId(category.getCategoryId())
                .orderPrice(orderPrice)
                .orderCount(orderCount)
                .orderType(OrderType.BUY)
                .build();

        // when
        OrderResponse orderResponse = orderService.createOrder(request);

        // then
        assertThat(orderResponse.getOrderId()).isNotNull();
        assertThat(orderResponse)
                .extracting("orderPrice", "orderCount", "orderType")
                .containsExactly(new BigDecimal("50000"), new BigDecimal("2"), OrderType.BUY);

        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(1)
                .extracting(Order::getOrderId, Order::getOrderCount, Order::getOrderType)
                .containsExactly(
                        tuple(orderResponse.getOrderId(), orderCount, OrderType.BUY)
                );

        ArgumentCaptor<BigDecimal> orderArgumentCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(assetService, times(1)).deductCash(
                eq(member.getMemberId()),
                orderArgumentCaptor.capture()
        );
        assertThat(orderArgumentCaptor.getValue()).isEqualByComparingTo(totalAmount);

    }

    @DisplayName("매수 주문을 하면 사용자의 현금이 차감된다.")
    @Test
    void createOrderDeductCash() {
        // given
        Member member = memberRepository.save(Member.builder()
                .memberEmail("test@test.com")
                .memberNickname("유진")
                .memberPassword("1234")
                .build());
        assetRepository.save(Asset.builder()
                .member(member)
                .assetCash(new BigDecimal("1000000"))
                .build());
        Category category = categoryRepository.save(Category.builder()
                .symbol("BTC")
                .build());

        OrderRequest request = OrderRequest.builder()
                .memberId(member.getMemberId())
                .categoryId(category.getCategoryId())
                .orderPrice(new BigDecimal("50000"))
                .orderCount(new BigDecimal("2"))
                .orderType(OrderType.BUY)
                .build();

        // when
        orderService.createOrder(request);

        // then
        verify(assetService).deductCash(eq(member.getMemberId()), eq(new BigDecimal("100000")));
    }


    @DisplayName("특정 회원의 미체결 주문 내역과 전체 개수를 조회한다.")
    @Test
    void getOpenOrderByMember() {
        // given
        Member member = memberRepository.save(Member.builder()
                .memberEmail("test@test.com")
                .memberNickname("유진")
                .memberPassword("1234")
                .build());

        Category category = categoryRepository.save(Category.builder()
                .symbol("BTC")
                .build());

        Order openOrder1    = createOrder(member, category, OrderStatus.OPEN,    "10000", "1");
        Order partialOrder1 = createOrder(member, category, OrderStatus.PARTIAL, "20000", "0.5");
        Order openOrder2    = createOrder(member, category, OrderStatus.OPEN,    "30000", "1");
        Order filledOrder1   = createOrder(member, category, OrderStatus.FILLED,  "40000", "0");

        orderRepository.saveAll(List.of(openOrder1, partialOrder1, openOrder2, filledOrder1));

        // when
        MemberOpenOrderResponse result = orderService.getOpenOrderByMember(member.getMemberId(), 0, 10);

        // then
        assertThat(result.getTotalOpenOrderCount()).isEqualTo(3L);
        assertThat(result.getOrders().getContent()).hasSize(3)
                .extracting(
                        o -> o.getOrderPrice().stripTrailingZeros().toPlainString(),
                        OrderResponse::getOrderStatus
                )
                .containsExactlyInAnyOrder(
                        tuple("10000", OrderStatus.OPEN),
                        tuple("20000", OrderStatus.PARTIAL),
                        tuple("30000", OrderStatus.OPEN)
                );
        assertThat(result.getOrders().getContent())
                .extracting("orderPrice")
                .doesNotContain(new BigDecimal("40000"));
    }

    @DisplayName("단일 주문 취소 시 한 건의 미체결 주문이 취소되고 환불된다.")
    @Test
    void cancelOrder() {
        // given
        Member member = memberRepository.save(Member.builder()
                .memberEmail("test@test.com")
                .memberNickname("유진")
                .memberPassword("1234")
                .build());

        Category category = categoryRepository.save(Category.builder()
                .symbol("BTC")
                .build());

        Order order = Order.builder()
                .member(member)
                .category(category)
                .orderPrice(new BigDecimal("20000"))
                .orderCount(new BigDecimal("1"))
                .remainingCount(new BigDecimal("0.5"))
                .orderType(OrderType.BUY)
                .orderStatus(OrderStatus.PARTIAL)
                .build();
        Order savedOrder = orderRepository.save(order);

        // when
        orderService.cancelOrder(savedOrder.getOrderId());

        // then
        verify(assetService, times(1)).refundCash(
                eq(member.getMemberId()),
                argThat(amount -> amount.compareTo(new BigDecimal("10000")) == 0)
        );
    }

    @DisplayName("전체 주문 취소 시 모든 미체결 주문이 취소 처리되고 환불된다.")
    @Test
    void cancelAllOrders() {
        // given
        Member member = memberRepository.save(Member.builder()
                .memberEmail("test@test.com")
                .memberNickname("유진")
                .memberPassword("1234")
                .build());

        Category category = categoryRepository.save(Category.builder()
                .symbol("BTC")
                .build());

        Order order1 = createOrder(member, category, OrderStatus.OPEN, "50000", "1");
        Order order2 = createOrder(member, category, OrderStatus.PARTIAL, "40000", "0.5");
        Order order3 = createOrder(member, category, OrderStatus.FILLED, "30000", "0");

        orderRepository.saveAll(List.of(order1, order2, order3));

        // when
        orderService.cancelAllOrders(member.getMemberId());

        // then
        List<Order> orders = orderRepository.findByMember_MemberIdOrderByOrderTimeDesc(member.getMemberId());
        assertThat(orders).hasSize(3)
                .extracting("orderStatus")
                .containsExactlyInAnyOrder(
                        OrderStatus.CANCELLED,
                        OrderStatus.CANCELLED,
                        OrderStatus.FILLED
                );
        verify(assetService, times(2)).refundCash(
                eq(member.getMemberId()),
                argThat(amount -> amount.compareTo(BigDecimal.ZERO) > 0));
    }


    /// 예외 상황
    @DisplayName("매수 주문 시 잔액이 부족하면 예외가 발생한다.")
    @Test
    void createOrderWithoutCash() {
        // given
        Member member = memberRepository.save(Member.builder()
                .memberEmail("test@test.com")
                .memberNickname("유진")
                .memberPassword("1234")
                .build());
        assetRepository.save(Asset.builder()
                .member(member)
                .assetCash(new BigDecimal("1000"))
                .build());
        Category category = categoryRepository.save(Category.builder()
                .symbol("BTC")
                .build());

        OrderRequest request = OrderRequest.builder()
                .memberId(member.getMemberId())
                .categoryId(category.getCategoryId())
                .orderPrice(new BigDecimal("50000"))
                .orderCount(new BigDecimal("1"))
                .orderType(OrderType.BUY)
                .build();


        doThrow(new IllegalArgumentException("잔액이 부족합니다."))
                .when(assetService).deductCash(eq(member.getMemberId()), any(BigDecimal.class));

        // when
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잔액이 부족합니다.");

        // then
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).isEmpty();
    }

//    @DisplayName("매도 주문 시 특정 종목에 대해 보유하고 있는 코인이 없을 경우 예외가 발생한다.")
//    @Test
//    void createOrderWithoutCategoryCoin() {
//        // given
//        Member member = memberRepository.save(Member.builder()
//                .memberEmail("test@test.com")
//                .memberNickname("유진")
//                .memberPassword("1234")
//                .build());
//        assetRepository.save(Asset.builder()
//                .member(member)
//                .assetCash(new BigDecimal("1000"))
//                .build());
//        Category category = categoryRepository.save(Category.builder()
//                .symbol("ETH")
//                .build());
//
//        OrderRequest request = OrderRequest.builder()
//                .memberId(member.getMemberId())
//                .categoryId(category.getCategoryId())
//                .orderPrice(new BigDecimal("50000"))
//                .orderCount(new BigDecimal("1"))
//                .orderType(OrderType.SELL)
//                .build();
//
//        // when
//        assertThatThrownBy(() -> orderService.createOrder(request))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessage("코인의 수량을 확인하세요.");
//
//        // then
//        List<Order> orders = orderRepository.findAll();
//        assertThat(orders).isEmpty();
//    }

    /// 다수 사용자
    @DisplayName("다수의 사용자가 동시에 주문을 생성한다.")
    @Test
    void manyMemberCreateOrder() {
        //given
        Member member = memberRepository.save(Member.builder()
                .memberEmail("test@test.com")
                .memberNickname("유진")
                .memberPassword("1234")
                .build());

        assetRepository.save(Asset.builder()
                .member(member)
                .assetCash(new BigDecimal("1000"))
                .build());
        Category category = categoryRepository.save(Category.builder()
                .symbol("BTC")
                .build());


        // when

        // then
    }


    private Order createOrder(Member member, Category category, OrderStatus status, String price, String remaining) {
        return Order.builder()
                .member(member)
                .category(category)
                .orderStatus(status)
                .orderPrice(new BigDecimal(price))
                .orderCount(new BigDecimal("1"))
                .remainingCount(new BigDecimal(remaining))
                .orderType(OrderType.BUY)
                .build();
    }
}