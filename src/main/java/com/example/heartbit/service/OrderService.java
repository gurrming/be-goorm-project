package com.example.heartbit.service;

import com.example.heartbit.disruptor.OrderCreatedEvent;
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

import java.math.BigDecimal;
import java.util.List;

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
    private final ApplicationEventPublisher eventPublisher;


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
        log.error("초기화 : {}개의 주문.", activeOrders.size());
    }

    public List<OrderBookResponse> getOrderBook(Long categoryId, OrderType orderType, int limit) {
        OrderBook book = orderBookCategory.getOrderBook(categoryId);
        return book.orderBookSnapshot(orderType, limit);
    }

    @Transactional
    public OrderResponse createOrder(@Valid OrderRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("카테고리를 찾을 수 없습니다."));

        Order newOrder = buildOrder(request, category);
        // 미체결 상태의 주문 저장
        Order savedOrder = orderRepository.saveAndFlush(newOrder);

        eventPublisher.publishEvent(new OrderCreatedEvent(savedOrder));

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
        if (request.getBotId() != null) {
            Bots bot = botsRepository.findById(request.getBotId())
                    .orElseThrow(() -> new IllegalArgumentException("봇을 찾을 수 없습니다."));

            return Order.builder()
                    .category(category)
                    .bots(bot)
                    .orderPrice(request.getOrderPrice())
                    .orderCount(request.getOrderCount())
                    .remainingCount(request.getOrderCount())
                    .orderType(request.getOrderType())
                    .orderStatus(OrderStatus.OPEN)
                    .build();
        } else {
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