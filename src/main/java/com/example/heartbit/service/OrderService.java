package com.example.heartbit.service;

import com.example.heartbit.domain.*;
import com.example.heartbit.dto.order.OrderBookResponse;
import com.example.heartbit.dto.order.OrderRequest;
import com.example.heartbit.dto.order.OrderResponse;
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
    private final TradeRepository tradeRepository;

    // 멤버별 주문 입력값
    @Transactional
    public OrderResponse createOrder(@Valid OrderRequest request) {

        // 매칭엔진과 연결

        return OrderResponse.builder()
                .orderId(request.getMemberId())
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

    // 추가
    // 호가창 - 매수 매도 목록
    public List<OrderBookResponse> getOrderBook(Long categoryId, OrderType orderType) {

        // 종가 가져오기
        BigDecimal lastClosingPrice = tradeRepository.findByCategory_CategoryIdOrderByTradeDateDesc(categoryId)
                .map(Trade::getTradeClosePrice)
                .orElse(BigDecimal.ZERO);

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

        // DTO 변환 및 등락률 계산
        return priceGroupMap.entrySet().stream()
                .map(entry -> {
                    BigDecimal currentPrice = entry.getKey();
                    double changeRate = 0.0;

                    // 전일 종가가 0이 아닐 때만 계산 (0 나누기 방지)
                    if (lastClosingPrice != null && lastClosingPrice.compareTo(BigDecimal.ZERO) > 0) {
                        changeRate = currentPrice.subtract(lastClosingPrice)
                                .divide(lastClosingPrice, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .doubleValue();
                    }

                    return OrderBookResponse.builder()
                            .orderPrice(currentPrice)
                            .totalRemainingCount(entry.getValue())
                            .changeRate(changeRate)
                            .build();
                })

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
