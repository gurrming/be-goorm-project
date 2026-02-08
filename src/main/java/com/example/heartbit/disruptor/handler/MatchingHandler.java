package com.example.heartbit.disruptor.handler;

import com.example.heartbit.disruptor.OrderEvent;
import com.example.heartbit.engine.core.MatchingEngine;
import com.example.heartbit.engine.core.OrderBook;
import com.example.heartbit.engine.core.OrderBookContainer;
import com.example.heartbit.engine.model.MatchResult;
import com.example.heartbit.engine.model.TradeCreateCommand;
import com.lmax.disruptor.EventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MatchingHandler implements EventHandler<OrderEvent> {
    private final OrderBookContainer container;
    private final MatchingEngine matchingEngine = new MatchingEngine();

    @Override
    public void onEvent(OrderEvent event, long seq, boolean endOfBatch) {
        OrderBook book = container.getOrderBook(event.getCategoryId());
        List<MatchResult> results = matchingEngine.match(book, event.getCommand());
        event.setResults(results);

        if (results != null && !results.isEmpty()) {
            List<TradeCreateCommand> tradeCommands = results.stream()
                    .map(result -> {
                        TradeCreateCommand cmd = TradeCreateCommand.from(result);
                        cmd.setCategoryId(event.getCategoryId());
                        cmd.setTakerType(event.getCommand().getType().name());
                        cmd.setTradeTime(java.time.LocalDateTime.now());

                        return cmd;
                    })
                    .toList();
            event.setTradeCommands(tradeCommands);
        }
    }
}