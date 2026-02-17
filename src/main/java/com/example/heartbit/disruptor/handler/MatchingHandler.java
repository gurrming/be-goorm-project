package com.example.heartbit.disruptor.handler;

import com.example.heartbit.disruptor.OrderEvent;
import com.example.heartbit.domain.OrderType;
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
    private final OrderBookCategory container;
    private final MatchingEngine matchingEngine = new MatchingEngine();

    @Override
    public void onEvent(OrderEvent event, long seq, boolean endOfBatch) {
        if (event.getCategoryId() == null) return;

        OrderBook book = container.getOrderBook(event.getCategoryId());

        if (event.getEventType() == OrderEvent.EventType.ORDER) {
            if (event.getCommand() != null) {
                List<MatchResult> results = matchingEngine.match(book, event.getCommand());
                event.setResults(results);

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
        }
        int limit = event.getLimit() > 0 ? event.getLimit() : 30;

        event.setBuySnapshot(book.orderBookSnapshot(OrderType.BUY, limit));
        event.setSellSnapshot(book.orderBookSnapshot(OrderType.SELL, limit));
    }
}