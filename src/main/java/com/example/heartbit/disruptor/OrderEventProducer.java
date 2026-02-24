package com.example.heartbit.disruptor;

import com.example.heartbit.domain.Order;
import com.example.heartbit.domain.OrderType;
import com.example.heartbit.dto.order.OrderBookResponse;
import com.example.heartbit.engine.model.OrderCommand;
import com.lmax.disruptor.RingBuffer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class OrderEventProducer {
    private final RingBuffer<OrderEvent> ringBuffer;

    public void publishOrder(Order order) {
        ringBuffer.publishEvent((event, sequence, arg) -> {
            event.clear();
            event.setEventType(OrderEvent.EventType.ORDER);
            event.setOrder(arg);
            event.setCategoryId(arg.getCategory().getCategoryId());
            event.setCommand(OrderCommand.from(arg));
        }, order);
    }

    public CompletableFuture<List<OrderBookResponse>> publishSnapshot(Long categoryId, OrderType type, int limit) {
        CompletableFuture<List<OrderBookResponse>> future = new CompletableFuture<>();

        ringBuffer.publishEvent((event, sequence) -> {
            event.clear();
            event.setEventType(OrderEvent.EventType.SNAPSHOT);
            event.setCategoryId(categoryId);
            event.setType(type);
            event.setLimit(limit);
            event.setSnapshotFuture(future); // 비동기 응답 통로 연결
        });

        return future;
    }
}
