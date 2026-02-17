package com.example.heartbit.disruptor.handler;

import com.example.heartbit.disruptor.OrderEvent;
import com.example.heartbit.dto.TradeResponse;
import com.example.heartbit.engine.model.TradeCreateCommand;
import com.example.heartbit.repository.OrderRepository;
import com.example.heartbit.service.TradeService;
import com.lmax.disruptor.EventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderDbHandler implements EventHandler<OrderEvent> {
    private final TradeService tradeService;
    private final OrderRepository orderRepository; // 조회 확인

    @Override
    public void onEvent(OrderEvent event, long seq, boolean endOfBatch) {
        if (event.getEventType() == OrderEvent.EventType.SNAPSHOT) return;

        List<TradeCreateCommand> commands = event.getTradeCommands();
        if (commands == null || commands.isEmpty()) return;

        waitForOrderVisibility(event.getCommand().getOrderId());

        List<TradeResponse> tradeResults = commands.stream()
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

        tradeService.processTradeResults(event.getCategoryId(), tradeResults);
    }

    private void waitForOrderVisibility(Long orderId) {
        int maxRetries = 5;
        int retryIntervalMs = 20;

        for (int i = 0; i < maxRetries; i++) {
            if (orderRepository.existsById(orderId)) {
                return;
            }
            try {
                Thread.sleep(retryIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}



