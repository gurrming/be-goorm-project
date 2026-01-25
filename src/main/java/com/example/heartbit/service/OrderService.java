package com.example.heartbit.service;

import com.example.heartbit.domain.*;
import com.example.heartbit.dto.TradeResponse;
import com.example.heartbit.dto.order.MemberOpenOrderResponse;
import com.example.heartbit.dto.order.OrderBookResponse;
import com.example.heartbit.dto.order.OrderRequest;
import com.example.heartbit.dto.order.OrderResponse;
import com.example.heartbit.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final TradeRepository tradeRepository;
    private final AssetRepository assetRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private final TradeEngineService tradeEngineService;
    private final AssetService assetService;
    private final TradeService tradeService;

    // 멤버별 주문 입력값
    @Transactional
    public OrderResponse createOrder(@Valid OrderRequest request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("멤버 정보를 찾을 수 없습니다."));

        // 자산 차감 로직 (매수일 때)
        if (request.getOrderType() == OrderType.BUY) {
            BigDecimal totalAmount = request.getOrderPrice().multiply(request.getOrderCount());
            assetService.blockCash(request.getMemberId(), totalAmount);
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

        if (tradeResults != null && !tradeResults.isEmpty()) {
            // Trade 생성
            tradeService.processTradeResults(newOrder.getCategory().getCategoryId(), tradeResults);
        }

        // saveAndFlush로 DB에 반영
        orderRepository.saveAndFlush(newOrder);

        // 호가창 업데이트
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
    public MemberOpenOrderResponse getOpenOrderByMember(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<OrderStatus> openStatus = List.of(OrderStatus.OPEN, OrderStatus.PARTIAL);

        // 페이징된 주문 리스트
        Slice<OrderResponse> orderSlice = orderRepository
                .findByMember_MemberIdAndOrderStatusInOrderByOrderTimeDesc(memberId, openStatus, pageable)
                .map(OrderResponse::from);

        // 미체결된 주문의 총 개수
        Long openOrderCount = orderRepository.countOpenOrdersByMember(memberId, openStatus);

        return MemberOpenOrderResponse.builder()
                .orders(orderSlice)
                .totalOpenOrderCount(openOrderCount != null ? openOrderCount : 0L)
                .build();
    }
    


    // 주문 취소
    private void processCancel(Order order) {
        if (order.getOrderStatus() != OrderStatus.OPEN && order.getOrderStatus() != OrderStatus.PARTIAL) {
            return;
        }
        // 매수 환불
        if (order.getOrderType() == OrderType.BUY) {
            BigDecimal refundAmount = order.getOrderPrice().multiply(order.getRemainingCount());
            assetService.restoreCash(order.getMember().getMemberId(), refundAmount);
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

    // 실시간 호가창 조회
    public void sendOrderBookUpdate(Long categoryId) {
        int limit = 30;

        // 현재가 기준 필터링된 호가 데이터 조회
        List<OrderBookResponse> buyOrderBook = getOrderBook(categoryId, OrderType.BUY, limit);
        List<OrderBookResponse> sellOrderBook = getOrderBook(categoryId, OrderType.SELL, limit);

        // 매도(SELL) 리스트 내림차순 정렬
        Collections.reverse(sellOrderBook);

        Map<String, Object> payload = Map.of(
                "categoryId", categoryId,
                "buySide", buyOrderBook,
                "sellSide", sellOrderBook,
                "serverTime", LocalDateTime.now().toString()
        );

        messagingTemplate.convertAndSend("/topic/orderbook/" + categoryId, (Object) payload);
    }


    // 호가창 조회
    public List<OrderBookResponse> getOrderBook(Long categoryId, OrderType orderType, int limit) {
        List<OrderStatus> activeStatuses = List.of(OrderStatus.OPEN, OrderStatus.PARTIAL);

        List<Order> orders = orderRepository
                .findByCategory_CategoryIdAndOrderTypeAndOrderStatusIn(categoryId, orderType, activeStatuses);

        return orders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getOrderPrice().setScale(0, RoundingMode.FLOOR),
                        Collectors.reducing(BigDecimal.ZERO, Order::getRemainingCount, BigDecimal::add)
                ))
                .entrySet().stream()
                .map(e -> OrderBookResponse.builder()
                        .orderPrice(integerOrderPrice(e.getKey()))
                        .totalRemainingCount(e.getValue())
                        .build()
                )
                // 정렬
                .sorted((o1, o2) -> orderType == OrderType.SELL
                        ? o1.getOrderPrice().compareTo(o2.getOrderPrice())
                        : o2.getOrderPrice().compareTo(o1.getOrderPrice()))
                .limit(limit)
                .collect(Collectors.toList());
    }


    // 소수점 아래 삭제 / 십의 자리 수는 소수점 한자리 / 일의 자리 수는 소수점 두자리
    private BigDecimal integerOrderPrice(BigDecimal price) {
        if (price.compareTo(BigDecimal.TEN) < 0) {
            return price.setScale(2, RoundingMode.FLOOR);
        } else if (price.compareTo(BigDecimal.valueOf(100)) < 0) {
            return price.setScale(1, RoundingMode.FLOOR);
        } else {
            return price.setScale(0, RoundingMode.FLOOR);
        }
    }

    public List<OrderBookResponse> getOrderBookWithPriceFilter(Long categoryId, OrderType orderType, int limit) {
        // 현재 체결가 조회
        BigDecimal lastPrice = tradeRepository.findTop1ByBuyOrder_Category_CategoryIdOrderByTradeTimeDesc(categoryId)
                .map(Trade::getTradePrice)
                .orElse(BigDecimal.ZERO);

        // 현재가 기준 정렬
        List<OrderBookResponse> result = getOrderBook(categoryId, orderType, limit);

        // SELL의 경우 리스트를 뒤집어 반환
        if (orderType == OrderType.SELL) {
            Collections.reverse(result);
        }
        return result;
    }
}
