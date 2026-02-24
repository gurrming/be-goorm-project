package com.example.heartbit.engine.core;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OrderBookCategory {
    private OrderBook[] orderBookArray;
    private int maxCategoryId = 0;

    @Getter
    private final MatchingEngine matchingEngine = new MatchingEngine();

    public void init(List<Long> categoryIds) {
        maxCategoryId = categoryIds.stream().mapToInt(Long::intValue).max().orElse(0);
        this.orderBookArray = new OrderBook[maxCategoryId + 1];

        for (Long categoryId : categoryIds) {
            orderBookArray[categoryId.intValue()] = new OrderBook();
        }

        // OrderBook 데이터를 unmodifiable 상태로 캡슐화
        // 다른 스레드에서 수정할 수 없도록 보호해주는 역할 v
        this.orderBooks = Collections.unmodifiableMap(tempMap);


    }

    public OrderBook getOrderBook(Long categoryId) {
        if (categoryId < 0 || categoryId > maxCategoryId) return null;
        return orderBookArray[categoryId.intValue()];
    }
}
