package com.example.heartbit.service;

import com.example.heartbit.domain.*;
import com.example.heartbit.dto.TradeResponse;
import com.example.heartbit.dto.order.MemberOpenOrderResponse;
import com.example.heartbit.dto.order.OrderRequest;
import com.example.heartbit.dto.order.OrderResponse;
import com.example.heartbit.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private BotsRepository botsRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private AssetService assetService;
    @Mock
    private OrderBookService orderBookService;
    @Mock
    private TradeEngineService tradeEngineService;
    @Mock
    private TradeService tradeService;

    @InjectMocks
    private OrderService orderService;

    private Member testMember;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testMember = Member.builder().memberId(1L).memberEmail("test@test.com").build();
        testCategory = Category.builder().categoryId(1L).symbol("BTC").build();
    }

    @DisplayName("종목에 대해 봇 ID로 주문하면 자산 차감 없이 주문이 생성된다.")
    @Test
    void createOrderWithBotId() {
        // given
        Bots bot = Bots.builder().botId(1L).build();
        Category category = Category.builder().categoryId(1L).symbol("BTC").build();

        OrderRequest request = OrderRequest.builder()
                .botId(1L)
                .categoryId(1L)
                .orderPrice(new BigDecimal("50000"))
                .orderCount(new BigDecimal("1"))
                .orderType(OrderType.BUY)
                .build();

        when(botsRepository.findById(1L)).thenReturn(Optional.of(bot));
        when(categoryRepository.findById(anyLong())).thenReturn(Optional.of(category));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // when
        orderService.createOrder(request);

        // then
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(assetService, never()).blockCash(anyLong(), any(BigDecimal.class));
    }

    @DisplayName("사용자가 주문을 생성하면 주문 데이터가 저장되고 자산 서비스의 차감 로직이 호출된다.")
    @Test
    void createOrder() {
        // given
        OrderRequest request = OrderRequest.builder()
                .memberId(1L)
                .categoryId(1L)
                .orderPrice(new BigDecimal("10000"))
                .orderCount(new BigDecimal("2"))
                .orderType(OrderType.BUY)
                .build();

        // stubbing
        when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        // save 시 null을 반환하지 않도록 설정
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // when
        orderService.createOrder(request);

        // then
        verify(assetService).blockCash(eq(1L), argThat(a -> a.compareTo(new BigDecimal("20000")) == 0));
        verify(orderRepository).save(any(Order.class));
    }

    @DisplayName("주문이 엔진에서 체결되면 해당 결과가 정산 서비스로 전달된다.")
    @Test
    void createOrderTradeResult() {
        // given
        Long categoryId = 1L;
        OrderRequest request = OrderRequest.builder()
                .memberId(1L)
                .categoryId(categoryId)
                .orderPrice(new BigDecimal("10000"))
                .orderCount(new BigDecimal("5"))
                .orderType(OrderType.BUY)
                .build();

        // stubbing
        when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        TradeResponse mockTrade = TradeResponse.builder()
                .tradePrice(new BigDecimal("10000"))
                .tradeCount(new BigDecimal("5"))
                .build();
        List<TradeResponse> tradeResults = List.of(mockTrade);

        when(tradeEngineService.processOrder(any(Order.class))).thenReturn(tradeResults);

        // when
        orderService.createOrder(request);

        // then
        verify(tradeService, times(1)).processTradeResults(eq(categoryId), eq(tradeResults));
        // 호가창 확인
        verify(orderBookService).broadcastOrderBook(eq(categoryId));
    }

    @DisplayName("체결 결과가 없으면 정산 서비스는 호출되지 않는다.")
    @Test
    void createOrder_shouldNotProcessSettlement_whenNoTrade() {
        // given
        OrderRequest request = OrderRequest.builder()
                .memberId(1L)
                .categoryId(1L)
                .orderPrice(new BigDecimal("10000"))
                .orderCount(new BigDecimal("5"))
                .orderType(OrderType.BUY)
                .build();

        when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // stubbing
        when(tradeEngineService.processOrder(any(Order.class))).thenReturn(Collections.emptyList());

        // when
        orderService.createOrder(request);

        // then
        verify(tradeService, never()).processTradeResults(anyLong(), anyList());
    }

    @DisplayName("특정 회원의 미체결 주문 내역과 전체 개수를 조회한다.")
    @Test
    void getOpenOrderByMember() {
        // given
        Long memberId = 1L;
        List<OrderStatus> openStatuses = List.of(OrderStatus.OPEN, OrderStatus.PARTIAL);

        Member member = Member.builder().memberId(memberId).build();
        Category category = Category.builder().categoryId(1L).build();

        Order o1 = createOrder(member, category, OrderStatus.OPEN, "10000", "1");
        Order o2 = createOrder(member, category, OrderStatus.PARTIAL, "20000", "0.5");

        List<Order> openOrders = List.of(o1, o2);
        Pageable pageable = PageRequest.of(0, 10);

        when(orderRepository.findByMember_MemberIdAndOrderStatusInOrderByOrderTimeDesc(eq(memberId), eq(openStatuses), any(Pageable.class)))
                .thenReturn(new PageImpl<>(openOrders));
        when(orderRepository.countOpenOrdersByMember(eq(memberId), eq(openStatuses))).thenReturn(2L);

        // when
        MemberOpenOrderResponse result = orderService.getOpenOrderByMember(memberId, 0, 10);

        // then
        assertThat(result.getTotalOpenOrderCount()).isEqualTo(2L);
        assertThat(result.getOrders().getContent()).hasSize(2)
                .extracting(o -> o.getOrderPrice().stripTrailingZeros().toPlainString(), OrderResponse::getOrderStatus)
                .containsExactly(tuple("10000", OrderStatus.OPEN), tuple("20000", OrderStatus.PARTIAL));
    }

    @DisplayName("미체결 주문이 없으면 0을 반환한다.")
    @Test
    void getOpenOrderByMember_empty() {
        // given
        Long memberId = 1L;
        List<OrderStatus> openStatuses = List.of(OrderStatus.OPEN, OrderStatus.PARTIAL);
        Pageable pageable = PageRequest.of(0, 10);

        // stubbing
        when(orderRepository.findByMember_MemberIdAndOrderStatusInOrderByOrderTimeDesc(anyLong(), anyList(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        when(orderRepository.countOpenOrdersByMember(anyLong(), anyList())).thenReturn(null);

        // when
        MemberOpenOrderResponse result = orderService.getOpenOrderByMember(memberId, 0, 10);

        // then
        assertThat(result.getTotalOpenOrderCount()).isEqualTo(0L); // 0L로 치환되는지 확인
        assertThat(result.getOrders().getContent()).isEmpty();
    }

    @DisplayName("단일 주문 취소 시 한 건의 미체결 주문이 취소되고 환불된다.")
    @Test
    void cancelOrder() {
        // given
        Long orderId = 1L;
        Order order = createOrder(testMember, testCategory, OrderStatus.PARTIAL, "20000", "0.5");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // when
        orderService.cancelOrder(orderId);

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(assetService).restoreCash(anyLong(), any(BigDecimal.class));
    }

    @DisplayName("전체 주문 취소 시 모든 미체결 주문이 취소 처리되고 환불된다.")
    @Test
    void cancelAllOrders() {
        // given
        Long memberId = 1L;
        Member member = Member.builder().memberId(memberId).build();
        Category category = Category.builder().categoryId(1L).build();

        Order o1 = createOrder(member, category, OrderStatus.OPEN, "50000", "1");
        Order o2 = createOrder(member, category, OrderStatus.PARTIAL, "40000", "0.5");
        Order o3 = createOrder(member, category, OrderStatus.FILLED, "30000", "0");

        when(orderRepository.findByMember_MemberIdOrderByOrderTimeDesc(memberId)).thenReturn(List.of(o1, o2, o3));

        // when
        orderService.cancelAllOrders(memberId);

        // then
        assertThat(o1.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(o2.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(o3.getOrderStatus()).isEqualTo(OrderStatus.FILLED);
        verify(assetService, times(2)).restoreCash(eq(memberId), any(BigDecimal.class));
    }

    @DisplayName("매수 주문 시 잔액이 부족하면 예외가 발생한다.")
    @Test
    void createOrderWithoutCash() {
        // given
        Long memberId = 1L;
        Member member = Member.builder().memberId(memberId).build();
        Category category = Category.builder().categoryId(1L).build();
        OrderRequest request = OrderRequest.builder().memberId(memberId).categoryId(1L)
                .orderPrice(new BigDecimal("50000")).orderCount(new BigDecimal("1")).orderType(OrderType.BUY).build();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(categoryRepository.findById(anyLong())).thenReturn(Optional.of(category));
        doThrow(new IllegalArgumentException("잔액이 부족합니다.")).when(assetService).blockCash(anyLong(), any(BigDecimal.class));

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잔액이 부족합니다.");
        verify(orderRepository, never()).save(any(Order.class));
    }

    private Order createOrder(Member member, Category category, OrderStatus status, String price, String remaining) {
        return Order.builder()
                .orderId(1L)
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