package com.example.heartbit.engine.core;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OrderBookContainer {
    private Map<Long, OrderBook> orderBooks;
    @Getter
    private final MatchingEngine matchingEngine = new MatchingEngine();

    public void init(List<Long> categoryIds) {
        Map<Long, OrderBook> tempMap = new HashMap<>();
        for (Long id : categoryIds) {
            tempMap.put(id, new OrderBook());
        }
        this.orderBooks = Collections.unmodifiableMap(tempMap);
    }

    public OrderBook getOrderBook(Long categoryId) {
        return orderBooks.get(categoryId);
    }

}
