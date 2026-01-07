package com.example.heartbit.service;

import com.example.heartbit.domain.*;
import com.example.heartbit.dto.TradeResponse;
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
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private final TradeEngineService tradeEngineService;
    private final AssetService assetService;
    private final TradeService tradeService;

    // 멤버별 주문 입력값
    @Transactional
    public OrderResponse createOrder(@Valid OrderRequest request) {

        Member member = memberRepository.findById(request.getMemberId()).orElseThrow(()-> new IllegalArgumentException("멤버 정보를 찾을 수 없습니다."));
        if (member.getMemberId().equals(1L)) {
            System.out.println("봇 주문");
        } else {
            // 진짜 사용자
            if (request.getOrderType() == OrderType.BUY) {
                BigDecimal totalAmount = request.getOrderPrice().multiply(request.getOrderCount());
                assetService.deductCash(member.getMemberId(), totalAmount);
            }
        }
        Category category = categoryRepository.findById(request.getCategoryId()).orElseThrow();

        Order newOrder = Order.builder()
                .member(member)
                .category(category)
                .orderPrice(request.getOrderPrice())
                .orderCount(request.getOrderCount())
                .remainingCount(request.getOrderCount())
                .orderType(request.getOrderType())
                .orderStatus(OrderStatus.OPEN)
                .build();
        orderRepository.save(newOrder);

        List<TradeResponse> tradeResults = tradeEngineService.processOrder(newOrder);
        if(!tradeResults.isEmpty()) {
            tradeService.processTradeResults(newOrder.getCategory().getCategoryId(), tradeResults);
        }

        sendOrderBookUpdate(request.getCategoryId());
        return OrderResponse.from(newOrder);
    }

    // 멤버별 주문 내역 리스트
    public List<OrderResponse> getOrderByMember(Long memberId) {
        return orderRepository.findByMember_MemberIdOrderByOrderTimeDesc(memberId).stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }

    // 회원 미체결 내역 리스트
    @Transactional(readOnly = true)
    public List<OrderResponse> getOpenOrderByMember(Long memberId) {
        List<OrderStatus> openStatus = List.of(OrderStatus.OPEN, OrderStatus.PARTIAL);

        return orderRepository.findByMember_MemberIdAndOrderStatusInOrderByOrderTimeDesc(memberId, openStatus)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    // 종목별 호가창
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
    // 주문 취소
    private void processCancel(Order order) {
        if (order.getOrderStatus() != OrderStatus.OPEN && order.getOrderStatus() != OrderStatus.PARTIAL) {
            return;
        }
        // 매수 환불
        if (order.getOrderType() == OrderType.BUY) {
            BigDecimal refundAmount = order.getOrderPrice().multiply(order.getRemainingCount());
            assetService.refundCash(order.getMember().getMemberId(), refundAmount);
        }
        order.cancel();
    }

    // 전체 주문 취소
    @Transactional
    public void cancelAllOrders(Long memberId) {
        List<Order> orders = orderRepository.findByMember_MemberIdOrderByOrderTimeDesc(memberId);
        //상태를 cancelled로 변경해줘야하는 로직 구현해야함.
        orders.forEach(this::processCancel);
    }

    // 주문 하나 취소
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
        //상태를 cancelled로 변경해줘야하는 로직 구현해야함.
        processCancel(order);
        sendOrderBookUpdate(order.getCategory().getCategoryId());
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
        // 취소 처리
        if (!expiredOrders.isEmpty()) {
            expiredOrders.forEach(order -> {
                try {
                    processCancel(order);
                } catch (Exception e) {}
            });
        }
    }


    // 호가창 (매수 매도 목록)
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

        // DTO 변환 (가격과 총 수량 정보만 포함)
        return priceGroupMap.entrySet().stream()
                .map(entry -> OrderBookResponse.builder()
                        .orderPrice(entry.getKey())
                        .totalRemainingCount(entry.getValue())
                        .build())

                // 가격 정렬 (매수: 높은 가격순, 매도: 낮은 가격순)
                .sorted((o1, o2) -> {
                    if (orderType == OrderType.BUY) {
                        return o2.getOrderPrice().compareTo(o1.getOrderPrice());
                    } else {
                        return o1.getOrderPrice().compareTo(o2.getOrderPrice());
                    }
                })
                .collect(Collectors.toList());
    }

}
