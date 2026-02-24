package com.example.heartbit.engine.core;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OrderBookCategory {
    private Map<Long, OrderBook> orderBooks;
    @Getter
    private final MatchingEngine matchingEngine = new MatchingEngine();

    public void init(List<Long> categoryIds) {
        Map<Long, OrderBook> tempMap = new HashMap<>();
        for (Long id : categoryIds) {
            tempMap.put(id, new OrderBook());
        }
        // OrderBook 데이터를 unmodifiable 상태로 캡슐화
        // 다른 스레드에서 수정할 수 없도록 보호해주는 역할 v
        this.orderBooks = Collections.unmodifiableMap(tempMap);
    }

    public OrderBook getOrderBook(Long categoryId) {
        return orderBooks.get(categoryId);
    }
}
