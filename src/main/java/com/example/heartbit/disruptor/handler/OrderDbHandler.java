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

    @Override
    public void onEvent(OrderEvent event, long seq, boolean endOfBatch) {
        if (event.getEventType() == OrderEvent.EventType.SNAPSHOT) return;

        List<TradeCreateCommand> commands = event.getTradeCommands();
        if (commands == null || commands.isEmpty()) return;

//        waitForOrderVisibility(event.getCommand().getOrderId());

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

    /// waitForOrderVisibility : 재시도 로직
    /// DB 커밋 전 엔진이 먼저 조회를 시도하여 NoSuchElementException이 발생
    /// 현재: OrderService에서 afterCommit을 적용하여 무조건 DB 저장 완료 후 이벤트가 넘어오므로
    /// 더 이상 불필요한 대기(Thread.sleep) 없이 즉시 처리가 가능함.
    /// 향후 개선: 엔진이 ID가 아닌 Order 객체 자체를 넘겨받으면 DB 접근 비용을 더 줄일 수 있음.
//    private void waitForOrderVisibility(Long orderId) {
//        int maxRetries = 5;
//        int retryIntervalMs = 20;
//
//        for (int i = 0; i < maxRetries; i++) {
//            if (orderRepository.existsById(orderId)) {
//                return;
//            }
//            try {
//                Thread.sleep(retryIntervalMs);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }
//    }
}



