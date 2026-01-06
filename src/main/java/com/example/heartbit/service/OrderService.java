package com.example.heartbit.service;

import com.example.heartbit.domain.*;
import com.example.heartbit.dto.TradeResponse;
import com.example.heartbit.dto.order.OrderBookResponse;
import com.example.heartbit.dto.order.OrderRequest;
import com.example.heartbit.dto.order.OrderResponse;
import com.example.heartbit.repository.CategoryRepository;
import com.example.heartbit.repository.MemberRepository;
import com.example.heartbit.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // =========================
    // 주문 생성
    // =========================
    @Transactional
    public OrderResponse createOrder(@Valid OrderRequest request) {
        // 주문 생성 및 저장
        //Member member = memberRepository.findById(request.getMemberId()).orElseThrow();
        //Category category = categoryRepository.findById(request.getCategoryId()).orElseThrow();
        //Order order = request.toEntity(member, category);
        //Order savedOrder = orderRepository.save(order);

//        sendOrderBookUpdate(request.getCategoryId());

//        // 매칭 엔진 호출
//        List<TradeResponse> tradeResults = tradeEngineService.processOrder(savedOrder);
//
//        // 체결 결과 DB 반영
//        if (!tradeResults.isEmpty()) {
//            processTradeResults(tradeResults);
//        }

        // 결과 반환
        return null;
    }


    // =========================
    // 멤버별 주문 조회
    // =========================
    public List<OrderResponse> getOrderByMember(Long memberId) {
        return orderRepository.findByMember_MemberIdOrderByOrderTimeDesc(memberId).stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }

    // =========================
    // 전체 주문 취소
    // =========================
    @Transactional
    public void cancelAllOrders(Long memberId) {
        List<Order> orders = orderRepository.findByMember_MemberIdOrderByOrderTimeDesc(memberId);
        orders.forEach(order -> {
            if (order.getOrderStatus() == OrderStatus.OPEN || order.getOrderStatus() == OrderStatus.PARTIAL) {
                order.cancel();
            }
        });
    }

    // =========================
    // 주문 하나 취소
    // =========================
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        order.cancel();

        sendOrderBookUpdate(order.getCategory().getCategoryId());
    }

    public void sendOrderBookUpdate(Long categoryId) {
        // 최신 매수/매도 호가 데이터를 가져옴
        List<OrderBookResponse> buyOrderBook = getOrderBook(categoryId, OrderType.BUY);
        List<OrderBookResponse> sellOrderBook = getOrderBook(categoryId, OrderType.SELL);

        // /topic/orderbook/{categoryId} 채널로 구독자 전원에게 전송
        Map<String, Object> payload = Map.of(
                "categoryId", categoryId,
                "buySide", buyOrderBook,
                "sellSide", sellOrderBook,
                "serverTime", LocalDateTime.now().toString()
        );
        String destination = "/topic/orderbook/" + categoryId;
        messagingTemplate.convertAndSend(destination, (Object) payload);
    }

    // =========================
    // 24시간 안에 체결되지 않은 주문 자동 취소
    // =========================
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void autoCancelExpiredOrders() {
        LocalDateTime expirationTime = LocalDateTime.now().minusHours(24);
        List<Order> expiredOrders = orderRepository.findExpiredOrders(expirationTime);

        expiredOrders.forEach(order -> {
            try {
                order.cancel();
            } catch (IllegalStateException e) {
                // 이미 체결된 주문은 무시
            }
        });

        if (!expiredOrders.isEmpty()) {
            System.out.println("24시간 경과 주문 " + expiredOrders.size() + "건 자동 취소 완료");
        }
    }

    // =========================
    // 호가창 조회
    // =========================
    public List<OrderBookResponse> getOrderBook(Long categoryId, OrderType orderType) {

        List<OrderStatus> activeStatuses = List.of(OrderStatus.OPEN, OrderStatus.PARTIAL);

        List<Order> orders = (orderType == OrderType.BUY)
                ? orderRepository.findByCategory_CategoryIdAndOrderTypeAndOrderStatusInOrderByOrderPriceDescOrderTimeAsc(categoryId, orderType, activeStatuses)
                : orderRepository.findByCategory_CategoryIdAndOrderTypeAndOrderStatusInOrderByOrderPriceAscOrderTimeAsc(categoryId, orderType, activeStatuses);

        Map<BigDecimal, BigDecimal> priceGroupMap = orders.stream()
                .collect(Collectors.groupingBy(Order::getOrderPrice,
                        Collectors.reducing(BigDecimal.ZERO, Order::getRemainingCount, BigDecimal::add)
                ));

        return priceGroupMap.entrySet().stream()
                .map(entry -> OrderBookResponse.builder()
                        .orderPrice(entry.getKey())
                        .totalRemainingCount(entry.getValue())
                        .build())
                .sorted((o1, o2) -> (orderType == OrderType.BUY)
                        ? o2.getOrderPrice().compareTo(o1.getOrderPrice())
                        : o1.getOrderPrice().compareTo(o2.getOrderPrice()))
                .collect(Collectors.toList());
    }
}
