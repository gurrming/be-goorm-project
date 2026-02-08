package com.example.heartbit.engine.core;

import com.example.heartbit.domain.OrderType;
import com.example.heartbit.dto.order.OrderBookResponse;
import com.example.heartbit.engine.model.OrderCommand;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class OrderBook {
    private final NavigableMap<BigDecimal, Deque<OrderCommand>> buy = new TreeMap<>(Comparator.reverseOrder());
    private final NavigableMap<BigDecimal, Deque<OrderCommand>> sell = new TreeMap<>();

    public void add(OrderCommand order) {
        var side = (order.getType() == OrderType.BUY) ? buy : sell;
        side.computeIfAbsent(order.getOrderPrice(), p -> new ArrayDeque<>()).addLast(order);
    }

    public List<OrderBookResponse> orderBookSnapshot(OrderType type, int limit) {
        NavigableMap<BigDecimal, Deque<OrderCommand>> side = (type == OrderType.BUY) ? buy : sell;

        if (side.isEmpty()) return new ArrayList<>();

        return side.entrySet().stream()
                .map(entry -> OrderBookResponse.builder()
                        .orderPrice(entry.getKey())
                        .totalRemainingCount(entry.getValue().stream()
                                .map(OrderCommand::getRemainingCount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                        .build())
                .filter(res -> res.getTotalRemainingCount().signum() > 0)
                .limit(limit)
                .collect(Collectors.toList());
    }

    public NavigableMap<BigDecimal, Deque<OrderCommand>> opposite(OrderType type) {
        return (type == OrderType.BUY) ? sell : buy;
    }
}