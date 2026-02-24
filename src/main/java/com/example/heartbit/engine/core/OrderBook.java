package com.example.heartbit.engine.core;

import com.example.heartbit.domain.OrderType;
import com.example.heartbit.dto.order.OrderBookResponse;
import com.example.heartbit.engine.model.OrderCommand;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class OrderBook {
    private final NavigableMap<BigDecimal, Queue<OrderCommand>> buy = new TreeMap<>(Comparator.reverseOrder());
    private final NavigableMap<BigDecimal, Queue<OrderCommand>> sell = new TreeMap<>();

    public void add(OrderCommand order) {
        if (order == null || order.getOrderPrice() == null) return;

        NavigableMap<BigDecimal, Queue<OrderCommand>> sideType = (order.getType() == OrderType.BUY) ? buy : sell;

        sideType.computeIfAbsent(order.getOrderPrice(), k -> new ArrayDeque<>()).add(order);
    }

    public List<OrderBookResponse> orderBookSnapshot(OrderType type, int limit) {
        NavigableMap<BigDecimal, Queue<OrderCommand>> sideType = (type == OrderType.BUY) ? buy : sell;

        if (sideType.isEmpty()) return new ArrayList<>();

        return sideType.entrySet().stream()
                .map(entry -> {
                    BigDecimal totalRemaining = entry.getValue().stream()
                            .map(o -> o.getRemainingCount() != null ? o.getRemainingCount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return OrderBookResponse.builder()
                            .orderPrice(entry.getKey())
                            .totalRemainingCount(totalRemaining)
                            .build();
                })
                .filter(res -> res.getTotalRemainingCount().compareTo(BigDecimal.ZERO) > 0)
                .limit(limit)
                .collect(Collectors.toList());
    }

    public NavigableMap<BigDecimal, Queue<OrderCommand>> opposite(OrderType type) {
        return (type == OrderType.BUY) ? sell : buy;
    }
}