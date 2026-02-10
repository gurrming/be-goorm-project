package com.example.heartbit.disruptor.handler;

import com.example.heartbit.disruptor.OrderEvent;
import com.example.heartbit.engine.core.MatchingEngine;
import com.example.heartbit.engine.core.OrderBook;
import com.example.heartbit.engine.core.OrderBookCategory;
import com.example.heartbit.engine.model.MatchResult;
import com.example.heartbit.engine.model.TradeCreateCommand;
import com.lmax.disruptor.EventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MatchingHandler implements EventHandler<OrderEvent> {
    private final OrderBookCategory container;
    private final MatchingEngine matchingEngine = new MatchingEngine();

    @Override
    public void onEvent(OrderEvent event, long seq, boolean endOfBatch) {
        OrderBook book = container.getOrderBook(event.getCategoryId());
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

            event.setTradeCommands(tradeCommands);
        }
    }
}