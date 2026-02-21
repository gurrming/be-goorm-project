package com.example.heartbit.controller;

import com.example.heartbit.disruptor.OrderEventProducer;
import com.example.heartbit.dto.order.OrderRequest;
import com.example.heartbit.domain.Order;
import com.example.heartbit.domain.Category;
import com.example.heartbit.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderRouter {
    private final OrderEventProducer orderEventProducer;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${shard.id:1}")
    private int shardId;

    @Value("${shard.channel:order-sharding-channel}")
    private String shardChannel;

    @Value("${shard.top-categories:1,2,3,4,5,6,8}")
    private List<Long> topCategories;

    public void route(OrderRequest request) {
        if (isMyShard(request.getCategoryId())) {
            // 현재 서버 담당인 경우: 엔티티 변환 후 기존 publishOrder 호출
            Order order = convertToEntity(request);
            orderEventProducer.publishOrder(order);
        } else {
            // 타 서버 담당인 경우: Redis로 전송
            redisTemplate.convertAndSend(shardChannel, request);
        }
    }

    private boolean isMyShard(Long categoryId) {
        if (categoryId == null) return false;
        boolean isTopCategory = topCategories.contains(categoryId);

        // 서버 1: 리스트에 있으면 내 담당
        // 서버 2: 리스트에 없으면 내 담당
        return (shardId == 1) ? isTopCategory : !isTopCategory;
    }

    private Order convertToEntity(OrderRequest request) {
        Category category = Category.builder()
                .categoryId(request.getCategoryId())
                .build();

        return Order.builder()
                .member(Member.builder().memberId(request.getMemberId()).build())
                .category(category)
                .orderPrice(request.getOrderPrice())
                .orderCount(request.getOrderCount())
                .orderType(request.getOrderType())
                .build();
    }
}