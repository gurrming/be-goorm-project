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
    }

    public OrderBook getOrderBook(Long categoryId) {
        if (categoryId < 0 || categoryId > maxCategoryId) return null;
        return orderBookArray[categoryId.intValue()];
    }
}
