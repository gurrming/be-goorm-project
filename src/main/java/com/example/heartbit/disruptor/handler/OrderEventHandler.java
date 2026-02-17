package com.example.heartbit.disruptor.handler;

import com.example.heartbit.disruptor.OrderEvent;
import com.example.heartbit.domain.OrderType;
import com.example.heartbit.dto.order.OrderBookResponse;
import com.example.heartbit.engine.core.OrderBook;
import com.example.heartbit.engine.core.OrderBookCategory;
import com.example.heartbit.service.OrderBookService;
import com.lmax.disruptor.EventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@RequiredArgsConstructor
public class OrderEventHandler implements EventHandler<OrderEvent> {
    private final OrderBookService orderBookService;

    @Override
    public void onEvent(OrderEvent event, long seq, boolean endOfBatch) {
        if (event.getEventType() == OrderEvent.EventType.SNAPSHOT) {
            if (event.getSnapshotFuture() != null) {
                event.getSnapshotFuture().complete(event.getBuySnapshot());
            }
            return;
        }

        if (event.getBuySnapshot() != null && event.getSellSnapshot() != null) {
            orderBookService.broadcastOrderBook(
                    event.getCategoryId(),
                    event.getBuySnapshot(),
                    event.getSellSnapshot()
            );
        }
    }
}