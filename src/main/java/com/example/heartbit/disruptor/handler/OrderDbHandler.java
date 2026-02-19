package com.example.heartbit.disruptor.handler;

import com.example.heartbit.disruptor.OrderEvent;
import com.example.heartbit.dto.trade.TradeResponse;
import com.example.heartbit.engine.model.TradeCreateCommand;
import com.example.heartbit.repository.OrderRepository;
import com.example.heartbit.service.TradeService;
import com.lmax.disruptor.EventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderDbHandler implements EventHandler<OrderEvent> {
    private final TradeService tradeService;
    private final List<TradeResponse> batchList = new ArrayList<>();

    @Override
    public void onEvent(OrderEvent event, long seq, boolean endOfBatch) {
        if (event.getEventType() == OrderEvent.EventType.SNAPSHOT) return;

        if (event.getTradeCommands() != null && !event.getTradeCommands().isEmpty()) {
            batchList.addAll(chunkTradeResponses(event));
        }

        if ((endOfBatch || batchList.size() >= 100) && !batchList.isEmpty()) {
            tradeService.processTradeResults(event.getCategoryId(), new ArrayList<>(batchList));
            batchList.clear();
        }
    }

    private List<TradeResponse> chunkTradeResponses(OrderEvent event) {
        return event.getTradeCommands().stream()
                .map(cmd -> TradeResponse.builder()
                        .buyOrderId(cmd.getBuyOrderId())
                        .sellOrderId(cmd.getSellOrderId())
                        .tradePrice(cmd.getPrice())
                        .tradeCount(cmd.getOrderCount())
                        .categoryId(cmd.getCategoryId())
                        .takerType(cmd.getTakerType())
                        .tradeTime(cmd.getTradeTime())
                        .build())
                .toList();
    }
}



