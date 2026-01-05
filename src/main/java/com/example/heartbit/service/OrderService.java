package com.example.heartbit.service;

import com.example.heartbit.domain.*;
import com.example.heartbit.dto.order.OrderBookResponse;
import com.example.heartbit.dto.order.OrderRequest;
import com.example.heartbit.dto.order.OrderResponse;
import com.example.heartbit.repository.CategoryRepository;
import com.example.heartbit.repository.MemberRepository;
import com.example.heartbit.repository.OrderRepository;
import com.example.heartbit.repository.TradeRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    // 멤버별 주문 입력값
    @Transactional
    public OrderResponse createOrder(@Valid OrderRequest request) {
        // 주문 생성
        Member member = memberRepository.findById(request.getMemberId()).orElseThrow();
        Category category = categoryRepository.findById(request.getCategoryId()).orElseThrow();

        // 직접 빌더를 쓰지 않고, Request에게 생성을 맡깁니다.
        Order order = request.toEntity(member, category);

        // 2. DB에 주문 저장
        Order savedOrder = orderRepository.save(order);

        // 3. 매칭 엔진과 연결 (체결 프로세스 시작)
        // matchingEngine.process(savedOrder);

        // 4. 저장된 결과를 Response DTO로 변환하여 반환
        return OrderResponse.from(savedOrder);
    }

    // 멤버별 주문 리스트
    public List<OrderResponse> getOrderByMember(Long memberId) {
        return orderRepository.findByMember_MemberIdOrderByOrderTimeDesc(memberId).stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }

    // 전체 주문 취소
    @Transactional
    public void cancelAllOrders(Long memberId) {
        List<Order> orders = orderRepository.findByMember_MemberIdOrderByOrderTimeDesc(memberId);
        orders.forEach(order -> {
            if (order.getOrderStatus() == OrderStatus.OPEN || order.getOrderStatus() == OrderStatus.PARTIAL) {
                //상태를 cancelled로 변경해줘야하는 로직 구현해야함.
                order.cancel();
            }
        });
    }

    // 주문 하나 취소
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        //상태를 cancelled로 변경해줘야하는 로직 구현해야함.
        order.cancel();
    }

    // 24시간 안에 체결되지 않으면 자동 취소
    //fixedDelay = 60000 1분으로 설정
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void autoCancelExpiredOrders() {
        // 현재 시간 기준으로 24시간 전 시점 계산
        LocalDateTime expirationTime = LocalDateTime.now().minusHours(24);

        // 24시간이 지났고, 아직 OPEN 또는 PARTIAL 상태인 주문들 조회
        List<Order> expiredOrders = orderRepository.findExpiredOrders(expirationTime);

        if (!expiredOrders.isEmpty()) {
            // 취소 처리
            expiredOrders.forEach(order -> {
                try {
                    order.cancel();
                } catch (IllegalStateException e) {
                    // 이미 FILLED된 주문이 조회 시점 차이로 섞였을 경우 대비
                }
            });
            // 24시간 경과 주문  expiredOrders.size()건 자동 취소 완료
            System.out.println("24시간 경과 주문 " + expiredOrders.size() + "건 자동 취소 완료");
        }
    }

    // 추가
    // 호가창 - 매수 매도 목록
    public List<OrderBookResponse> getOrderBook(Long categoryId, OrderType orderType) {

        List<OrderStatus> activeStatuses = List.of(OrderStatus.OPEN, OrderStatus.PARTIAL);

        // 주문 목록 조회 (작성한 정렬 순서대로 가져옴)
        List<Order> orders = (orderType == OrderType.BUY)
                ? orderRepository.findByCategory_CategoryIdAndOrderTypeAndOrderStatusInOrderByOrderPriceDescOrderTimeAsc(categoryId, orderType, activeStatuses)
                : orderRepository.findByCategory_CategoryIdAndOrderTypeAndOrderStatusInOrderByOrderPriceAscOrderTimeAsc(categoryId, orderType, activeStatuses);

        // 가격별 잔량(remainingCount) 합산
        Map<BigDecimal, BigDecimal> priceGroupMap = orders.stream()
                .collect(Collectors.groupingBy(Order::getOrderPrice,
                        Collectors.reducing(BigDecimal.ZERO, Order::getRemainingCount, BigDecimal::add)
                ));

        // 3. DTO 변환 (가격과 총 수량 정보만 포함)
        return priceGroupMap.entrySet().stream()
                .map(entry -> OrderBookResponse.builder()
                        .orderPrice(entry.getKey())
                        .totalRemainingCount(entry.getValue())
                        .build())

                // 4. 가격 정렬 (매수는 높은 가격순, 매도는 낮은 가격순)
                .sorted((o1, o2) -> {
                    if (orderType == OrderType.BUY) {
                        return o2.getOrderPrice().compareTo(o1.getOrderPrice()); // 매수: 높은 가격순
                    } else {
                        return o1.getOrderPrice().compareTo(o2.getOrderPrice()); // 매도: 낮은 가격순
                    }
                })
                .collect(Collectors.toList());
    }
}
