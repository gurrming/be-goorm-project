package com.example.heartbit.disruptor;

import com.example.heartbit.domain.Order;
import com.example.heartbit.engine.model.OrderCommand;
import com.lmax.disruptor.RingBuffer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventProducer {
    private final RingBuffer<OrderEvent> ringBuffer;

    public void publish(Order order) {
        long seq = ringBuffer.next();
        try {
            OrderEvent event = ringBuffer.get(seq);
            event.clear();
            event.setCategoryId(order.getCategory().getCategoryId());
            event.setCommand(OrderCommand.from(order));

        } finally {
            ringBuffer.publish(seq);
        }
    }
}
