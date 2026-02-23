package com.example.heartbit.service;


import com.example.heartbit.domain.Category;
import com.example.heartbit.engine.core.OrderBook;
import com.example.heartbit.engine.core.OrderBookCategory;
import com.example.heartbit.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TradeInitializer {

    private final TradeService tradeService;
    private final OrderBookCategory orderBookCategory;
    private final CategoryRepository categoryRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void initData() {
        List<Long> categoryIds = categoryRepository.findAll().stream()
                .map(Category::getCategoryId)
                .collect(Collectors.toList());

        orderBookCategory.init(categoryIds);

        tradeService.init();
    }
}
