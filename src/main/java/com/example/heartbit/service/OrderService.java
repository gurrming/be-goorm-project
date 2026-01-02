package com.example.heartbit.service;

import com.example.heartbit.domain.Order;
import com.example.heartbit.domain.OrderStatus;
import com.example.heartbit.dto.OrderRequest;
import com.example.heartbit.dto.OrderResponse;
import com.example.heartbit.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;

    // 멤버별 주문 입력값
    @Transactional
    public OrderResponse createOrder(@Valid OrderRequest request) {

        // 매칭엔진과 연결

        return OrderResponse.builder()
                .orderId(1L)
                .orderStatus(OrderStatus.OPEN)
                .build();
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


}
