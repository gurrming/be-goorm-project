package com.example.heartbit.disruptor.handler;

import com.example.heartbit.disruptor.OrderEvent;
import com.example.heartbit.service.TradeService;
import com.lmax.disruptor.EventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderDbHandler implements EventHandler<OrderEvent> {
    private final TradeService tradeService;

    @Override
    public void onEvent(OrderEvent event, long seq, boolean endOfBatch) {
        if (event.getTradeCommands() == null || event.getTradeCommands().isEmpty()) {
            return;
        }
        tradeService.processTradeResults(
                event.getCategoryId(),
                event.getTradeCommands()
        );
    }
}



