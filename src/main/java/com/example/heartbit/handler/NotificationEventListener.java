package com.example.heartbit.handler;

import com.example.heartbit.domain.NotificationType;
import com.example.heartbit.domain.Order;
import com.example.heartbit.dto.trade.TradeNotificationEvent;
import com.example.heartbit.dto.trade.TradeResponse;
import com.example.heartbit.repository.OrderRepository;
import com.example.heartbit.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final OrderRepository orderRepository;

    @Async // 비동기
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleTradeNotification(TradeNotificationEvent event) {
        int scale = 3;
        RoundingMode mode = RoundingMode.HALF_UP;

        for (TradeResponse response : event.getTradeResults()) {
            try {
                // 알림 문구를 위해 최신 주문 상태 조회
                Order buyOrder = orderRepository.findById(response.getBuyOrderId()).orElse(null);
                Order sellOrder = orderRepository.findById(response.getSellOrderId()).orElse(null);

                // 1. 매수자 알림
                if (buyOrder != null && buyOrder.getMember() != null) {
                    sendTradeMsg(buyOrder, response.getTradeCount(), "매수", scale, mode);
                }

                // 2. 매도자 알림
                if (sellOrder != null && sellOrder.getMember() != null) {
                    sendTradeMsg(sellOrder, response.getTradeCount(), "매도", scale, mode);
                }

                // 3. 시세 변동 알림 (checkAndSendPriceAlert 호출)
                notificationService.checkAndSendPriceAlert(
                        event.getCategoryId(),
                        response.getTradePrice(),
                        buyOrder != null ? buyOrder.getCategory().getCategoryName() : "Unknown",
                        event.getReferencePrice()
                );

            } catch (Exception e) {
                log.error("[알림 리스너 오류] 체결 결과 처리 중 예외 발생: {}", e.getMessage());
            }
        }
    }

    private void sendTradeMsg(Order order, BigDecimal tradeCount, String type, int scale, RoundingMode mode) {
        String tradeCountStr = tradeCount.setScale(scale, mode).toPlainString();
        String remainingCountStr = order.getRemainingCount().setScale(scale, mode).toPlainString();
        String categoryName = order.getCategory().getCategoryName();

        String msg;
        if (order.getRemainingCount().compareTo(BigDecimal.ZERO) > 0) {
            msg = String.format("[%s] %s 부분 체결! (%s주 체결되었고, %s주 남았어요.)",
                    categoryName, type, tradeCountStr, remainingCountStr);
        } else {
            msg = String.format("[%s] %s 체결 완료! (총 %s주)",
                    categoryName, type, tradeCountStr);
        }
        notificationService.send(order.getMember(), msg, NotificationType.TRADE);
    }
}
