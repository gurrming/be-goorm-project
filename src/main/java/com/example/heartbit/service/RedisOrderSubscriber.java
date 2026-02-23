package com.example.heartbit.service;

import com.example.heartbit.disruptor.OrderEventProducer;
import com.example.heartbit.dto.order.OrderRequest;
import com.example.heartbit.domain.Order;
import com.example.heartbit.domain.Category;
import com.example.heartbit.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisOrderSubscriber {
    private final OrderEventProducer orderEventProducer;

    @Value("${shard.id:1}")
    private int shardId;

    @Value("${shard.top-categories:1,2,3,4,5,6,8}")
    private List<Long> topCategories;

    public void handleMessage(OrderRequest request) {
        if (isMyShard(request.getCategoryId())) {
            Order order = convertToEntity(request);
            orderEventProducer.publishOrder(order);
        }
    }

    private boolean isMyShard(Long categoryId) {
        if (categoryId == null) return false;
        boolean isTopCategory = topCategories.contains(categoryId);
        return (shardId == 1) ? isTopCategory : !isTopCategory;
    }

    private Order convertToEntity(OrderRequest request) {
        return Order.builder()
                .member(Member.builder().memberId(request.getMemberId()).build())
                .category(Category.builder().categoryId(request.getCategoryId()).build())
                .orderPrice(request.getOrderPrice())
                .orderCount(request.getOrderCount())
                .orderType(request.getOrderType())
                .build();
    }
}