package com.example.heartbit.service;

import com.example.heartbit.domain.*;
import com.example.heartbit.dto.order.OrderBookResponse;
import com.example.heartbit.dto.order.OrderRequest;
import com.example.heartbit.dto.order.OrderResponse;
import com.example.heartbit.repository.CategoryRepository;
import com.example.heartbit.repository.MemberRepository;
import com.example.heartbit.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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

    // =========================
    // 주문 생성
    // =========================
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {

        // 카테고리 조회
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));

        // 회원 조회 (BOT이면 member는 null)
        Member member = null;
        if (!Boolean.TRUE.equals(request.getIsBot())) {
            if (request.getMemberId() == null) {
                throw new IllegalArgumentException("USER 주문에는 memberId가 필요합니다.");
            }
            member = memberRepository.findById(request.getMemberId())
                    .orElseThrow(() -> new EntityNotFoundException("Member not found"));
        }

        // ✅ Order 엔티티 생성
        Order order = request.toEntity(member, category);

        // ✅ 저장
        Order savedOrder = orderRepository.save(order);

        // ✅ Response 반환
        return OrderResponse.from(savedOrder);
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
