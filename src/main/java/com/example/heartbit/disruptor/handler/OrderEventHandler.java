package com.example.heartbit.disruptor.handler;

import com.example.heartbit.disruptor.OrderEvent;
import com.example.heartbit.domain.OrderType;
import com.example.heartbit.dto.order.OrderBookResponse;
import com.example.heartbit.engine.core.OrderBook;
import com.example.heartbit.engine.core.OrderBookContainer;
import com.example.heartbit.service.OrderBookService;
import com.lmax.disruptor.EventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@RequiredArgsConstructor
public class OrderEventHandler implements EventHandler<OrderEvent> {
    private final OrderBookContainer container;
    private final OrderBookService orderBookService;

    @Override
    public void onEvent(OrderEvent event, long seq, boolean endOfBatch) {
        OrderBook book = container.getOrderBook(event.getCategoryId());

        List<OrderBookResponse> buySnap = book.orderBookSnapshot(OrderType.BUY, 30);
        List<OrderBookResponse> sellSnap = book.orderBookSnapshot(OrderType.SELL, 30);

        orderBookService.broadcastOrderBook(event.getCategoryId(), buySnap, sellSnap);
    }
}