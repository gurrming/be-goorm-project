package com.example.heartbit.disruptor.handler;

import com.example.heartbit.disruptor.OrderEvent;
import com.example.heartbit.domain.OrderType;
import com.example.heartbit.dto.order.OrderBookResponse;
import com.example.heartbit.engine.core.MatchingEngine;
import com.example.heartbit.engine.core.OrderBook;
import com.example.heartbit.engine.core.OrderBookCategory;
import com.example.heartbit.engine.model.MatchResult;
import com.example.heartbit.engine.model.TradeCreateCommand;
import com.lmax.disruptor.EventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MatchingHandler implements EventHandler<OrderEvent> {
    private final OrderBookCategory orderBookCategory;
    private final MatchingEngine matchingEngine = new MatchingEngine();

    @Override
    public void onEvent(OrderEvent event, long seq, boolean endOfBatch) {
        if (event.getCategoryId() == null) return;

        OrderBook book = orderBookCategory.getOrderBook(event.getCategoryId());
        if (event.getEventType() == OrderEvent.EventType.ORDER) {
            handleOrderEvent(event, book);
        }
        else if (event.getEventType() == OrderEvent.EventType.SNAPSHOT) {
            handleSnapshotEvent(event, book);
        }
    }

    private void handleOrderEvent(OrderEvent event, OrderBook book) {
        if (event.getCommand() == null) return;

        List<MatchResult> results = matchingEngine.match(book, event.getCommand());
        event.setResults(results);

        event.setBuySnapshot(book.orderBookSnapshot(OrderType.BUY, 30));
        event.setSellSnapshot(book.orderBookSnapshot(OrderType.SELL, 30));

        if (results != null && !results.isEmpty()) {
            List<TradeCreateCommand> tradeCommands = results.stream()
                    .map(result -> TradeCreateCommand.from(
                            result,
                            event.getCategoryId(),
                            event.getCommand().getType().name()
                    ))
                    .toList();
            event.setTradeCommands(new ArrayList<>(tradeCommands));
        }
    }

    private void handleSnapshotEvent(OrderEvent event, OrderBook book) {
        if (event.getSnapshotFuture() == null) return;

        int limit = event.getLimit() > 0 ? event.getLimit() : 30;

        List<OrderBookResponse> snapshot = book.orderBookSnapshot(event.getType(), limit);
        event.getSnapshotFuture().complete(snapshot);
    }
}