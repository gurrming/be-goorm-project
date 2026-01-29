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
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
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
    private final BotsRepository  botsRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private final TradeEngineService tradeEngineService;
    private final AssetService assetService;
    private final TradeService tradeService;
    private final OrderBookService orderBookService;
    // 멤버별 주문 입력값
    @Transactional
    public OrderResponse createOrder(@Valid OrderRequest request) {
        // 종목 조회
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("카테고리를 찾을 수 없습니다."));

        Order newOrder;

        // 주문 주체 판별
        if (request.getBotId() != null) {
            // 봇 주문 처리
            Bots bot = botsRepository.findById(request.getBotId())
                    .orElseThrow(() -> new IllegalArgumentException("봇을 찾을 수 없습니다."));

            newOrder = Order.builder()
                    .category(category)
                    .bots(bot)
                    .orderPrice(request.getOrderPrice())
                    .orderCount(request.getOrderCount())
                    .remainingCount(request.getOrderCount())
                    .orderType(request.getOrderType())
                    .orderStatus(OrderStatus.OPEN)
                    .build();
        } else {
            // 일반 회원 주문 처리
            Member member = memberRepository.findById(request.getMemberId())
                    .orElseThrow(() -> new IllegalArgumentException("멤버 정보를 찾을 수 없습니다."));

        // 자산 차감 로직 (매수일 때)
        if (request.getOrderType() == OrderType.BUY) {
            BigDecimal totalAmount = request.getOrderPrice().multiply(request.getOrderCount());
            assetService.blockCash(request.getMemberId(), totalAmount);
        }

            newOrder = Order.builder()
                    .category(category)
                    .member(member)
                    .orderPrice(request.getOrderPrice())
                    .orderCount(request.getOrderCount())
                    .remainingCount(request.getOrderCount())
                    .orderType(request.getOrderType())
                    .orderStatus(OrderStatus.OPEN)
                    .build();
        }

        // DB 저장 및 매칭 엔진 전달
        Order savedOrder = orderRepository.save(newOrder);
        orderRepository.flush(); // 엔진에서 즉시 조회가 필요할 경우를 대비

        // 엔진 매칭
        List<TradeResponse> tradeResults = tradeEngineService.processOrder(savedOrder);

        // 체결 발생 시 정산 처리
        if (tradeResults != null && !tradeResults.isEmpty()) {
            tradeService.processTradeResults(category.getCategoryId(), tradeResults);
        }

        // 엔진 메모리 기반 호가창 실시간 전송
        orderBookService.broadcastOrderBook(category.getCategoryId());

        return OrderResponse.from(savedOrder);
    }

//    // 회원 주문 리스트
//    public List<OrderResponse> getOrderByMember(Long memberId) {
//        return orderRepository.findByMember_MemberIdOrderByOrderTimeDesc(memberId).stream()
//                .map(OrderResponse::from)
//                .collect(Collectors.toList());
//    }

    // 회원 미체결 내역 리스트
    @Transactional(readOnly = true)
    public MemberOpenOrderResponse getOpenOrderByMember(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<OrderStatus> openStatus = List.of(OrderStatus.OPEN, OrderStatus.PARTIAL);

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
        if (order.getOrderType() == OrderType.BUY && order.getMember() != null) {
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
        orders.stream()
                .map(o -> o.getCategory().getCategoryId())
                .distinct()
                .forEach(orderBookService::broadcastOrderBook);
    }

    // 주문 하나 취소
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다."));
        //상태를 cancelled로 변경해줘야하는 로직 구현해야함.
        processCancel(order);
        orderBookService.broadcastOrderBook(order.getCategory().getCategoryId());
    }


}
