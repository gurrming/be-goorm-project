package com.example.heartbit.service;

import com.example.heartbit.disruptor.OrderEventProducer;
import com.example.heartbit.domain.*;
import com.example.heartbit.dto.order.*;
import com.example.heartbit.engine.core.MatchingEngine;
import com.example.heartbit.engine.core.OrderBook;
import com.example.heartbit.engine.core.OrderBookCategory;
import com.example.heartbit.dto.order.OrderBookResponse;
import com.example.heartbit.engine.model.OrderCommand;
import com.example.heartbit.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final BotsRepository botsRepository;

    private final AssetService assetService;
    private final OrderBookService orderBookService;
    private final OrderBookCategory orderBookCategory;
    private final OrderEventProducer orderEventProducer;


    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void initOrderBook() {
        List<Long> categoryIds = categoryRepository.findAll().stream()
                .map(Category::getCategoryId).toList();
        orderBookCategory.init(categoryIds);

        List<Order> activeOrders = orderRepository.findByOrderStatusInOrderByOrderTimeAsc(
                List.of(OrderStatus.OPEN, OrderStatus.PARTIAL));

        MatchingEngine matchingEngine = orderBookCategory.getMatchingEngine();

        for (Order order : activeOrders) {
            OrderBook book = orderBookCategory.getOrderBook(order.getCategory().getCategoryId());

            matchingEngine.match(book, OrderCommand.from(order));
        }
    }

    public List<OrderBookResponse> getOrderBook(Long categoryId, OrderType orderType, int limit) {
        try {
            return orderEventProducer.publishSnapshot(categoryId, orderType, limit)
                    .get(500, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Transactional
    public OrderResponse createOrder(@Valid OrderRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId()).orElseThrow();

        Order savedOrder = orderRepository.saveAndFlush(buildOrder(request, category));

        // orderEventProducer.publishOrder(savedOrder);

        /// API 스레드의 DB 커밋 완료 시점과 엔진 스레드의 조회 시점 차이로 인한 '가시성 에러' 방지.
        /// 롤백 시 엔진에 이벤트가 발행되는 것을 막아 데이터 무결성 보장.
        /// 트랜잭션 성공 시에만 엔진을 가동.
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    orderEventProducer.publishOrder(savedOrder);
                }
            });
        } else {
            orderEventProducer.publishOrder(savedOrder);
        }

        return OrderResponse.from(savedOrder);
    }

    @Transactional(readOnly = true)
    public MemberOpenOrderResponse getOpenOrderByMember(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<OrderStatus> openStatus = List.of(OrderStatus.OPEN, OrderStatus.PARTIAL);

        Slice<OrderResponse> orderSlice = orderRepository
                .findByMember_MemberIdAndOrderStatusInOrderByOrderTimeDesc(memberId, openStatus, pageable)
                .map(OrderResponse::from);

        Long openOrderCount = orderRepository.countOpenOrdersByMember(memberId, openStatus);

        return MemberOpenOrderResponse.builder()
                .orders(orderSlice)
                .totalOpenOrderCount(openOrderCount != null ? openOrderCount : 0L)
                .build();
    }

    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다."));

        processCancel(order);
        broadcastOrderBook(order.getCategory().getCategoryId());
    }

    @Transactional
    public void cancelAllOrders(Long memberId) {
        List<Order> orders = orderRepository.findByMember_MemberIdOrderByOrderTimeDesc(memberId);
        orders.forEach(this::processCancel);

        orders.stream()
                .map(o -> o.getCategory().getCategoryId())
                .distinct()
                .forEach(this::broadcastOrderBook);
    }

    private Order buildOrder(OrderRequest request, Category category) {
        if (request.getMemberId() != null) {
            Member member = memberRepository.findById(request.getMemberId())
                    .orElseThrow(() -> new IllegalArgumentException("멤버 정보를 찾을 수 없습니다."));

            if (request.getOrderType() == OrderType.BUY) {
                BigDecimal totalAmount = request.getOrderPrice().multiply(request.getOrderCount());
                assetService.blockCash(request.getMemberId(), totalAmount);
            }
            return Order.builder()
                    .category(category)
                    .member(member)
                    .orderPrice(request.getOrderPrice())
                    .orderCount(request.getOrderCount())
                    .remainingCount(request.getOrderCount())
                    .orderType(request.getOrderType())
                    .orderStatus(OrderStatus.OPEN)
                    .build();
        } else {
            Bots bot;
            if(request.getBotId() != null) {
                bot = botsRepository.findById(request.getBotId())
                        .orElseThrow(() -> new IllegalArgumentException("봇 정보를 찾을 수 없습니다."));
            } else {
                bot = botsRepository.save(Bots.builder().build());
            }
            return Order.builder()
                    .category(category)
                    .bots(bot)
                    .orderPrice(request.getOrderPrice())
                    .orderCount(request.getOrderCount())
                    .remainingCount(request.getOrderCount())
                    .orderType(request.getOrderType())
                    .orderStatus(OrderStatus.OPEN)
                    .build();
        }
    }

    private void processCancel(Order order) {
        if (order.getOrderStatus() != OrderStatus.OPEN && order.getOrderStatus() != OrderStatus.PARTIAL) {
            return;
        }
        if (order.getOrderType() == OrderType.BUY && order.getMember() != null) {
            BigDecimal refundAmount = order.getOrderPrice().multiply(order.getRemainingCount());
            assetService.restoreCash(order.getMember().getMemberId(), refundAmount);
        }
        order.cancel();
    }

    private void broadcastOrderBook(Long categoryId) {
        OrderBook book = orderBookCategory.getOrderBook(categoryId);
        orderBookService.broadcastOrderBook(
                categoryId,
                book.orderBookSnapshot(OrderType.BUY, 30),
                book.orderBookSnapshot(OrderType.SELL, 30)
        );
    }
}