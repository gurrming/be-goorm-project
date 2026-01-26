package com.example.heartbit.config;

import com.example.heartbit.domain.Order;
import com.example.heartbit.domain.OrderStatus;
import com.example.heartbit.dto.TradeResponse;
import com.example.heartbit.repository.OrderRepository;
import com.example.heartbit.service.TradeEngineService;
import com.example.heartbit.service.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class TradeEngineConfig implements CommandLineRunner {

    private final OrderRepository orderRepository;
    private final TradeEngineService tradeEngineService;
    private final TradeService tradeService;

    @Override
    @Transactional
    public void run(String... args) {

        // DB에서 미체결, 부분체결 주문 조회 (오래된 순서대로)
        List<OrderStatus> activeStatuses = List.of(OrderStatus.OPEN, OrderStatus.PARTIAL);
        List<Order> activeOrders = orderRepository.findByOrderStatusInOrderByOrderTimeAsc(activeStatuses);

        if (activeOrders.isEmpty()) {
            return;
        }

        // 엔진 메모리에 주문 적재
        for (Order order : activeOrders) {
            order.setOrderPrice(tradeEngineService.normalizePrice(order.getOrderPrice()));
            tradeEngineService.processOrder(order);
        }
    }
}