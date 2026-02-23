package com.example.heartbit.handler;

import com.example.heartbit.domain.Member;
import com.example.heartbit.domain.Notification;
import com.example.heartbit.domain.NotificationType;
import com.example.heartbit.dto.NotificationResponseDto;
import com.example.heartbit.dto.trade.TradeNotificationEvent;
import com.example.heartbit.repository.NotificationRepository;
import com.example.heartbit.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleTradeNotification(TradeNotificationEvent event) {
        log.info("리스너 수신 성공! categoryId: {}", event.getCategoryId());
        if (event.getDetails() == null || event.getDetails().isEmpty()) return;

        List<Notification> bulkList = new ArrayList<>();
        int scale = 3;
        RoundingMode mode = RoundingMode.HALF_UP;

        for (TradeNotificationEvent.NotificationDetail detail : event.getDetails()) {
            try {

                String msg = createTradeMessage(detail, scale, mode);

                Member member = Member.builder().memberId(detail.getMemberId()).build();
                Notification notification = new Notification(member, msg, NotificationType.TRADE);
                bulkList.add(notification);

                messagingTemplate.convertAndSend("/topic/notification/" + detail.getMemberId(),
                        NotificationResponseDto.from(notification));

            } catch (Exception e) {
                log.error("[체결 알림 생성 오류] memberId: {}, 에러: {}", detail.getMemberId(), e.getMessage());
            }
        }

        // 2. 시세 변동 알림 처리
        try {
            var first = event.getDetails().get(0);

            List<Notification> priceAlerts = notificationService.createPriceAlerts(
                    event.getCategoryId(),
                    first.getTradePrice(),
                    first.getCategoryName(),
                    event.getReferencePrice()
            );

            for (Notification n : priceAlerts) {
                messagingTemplate.convertAndSend("/topic/notification/" + n.getMember().getMemberId(),
                        NotificationResponseDto.from(n));
            }

            bulkList.addAll(priceAlerts);

        } catch (Exception e) {
            log.error("[시세 알림 처리 오류] categoryId: {}, 에러: {}", event.getCategoryId(), e.getMessage());
        }

        if (!bulkList.isEmpty()) {
            notificationRepository.saveAll(bulkList);
        }
    }

    private String createTradeMessage(TradeNotificationEvent.NotificationDetail detail, int scale, RoundingMode mode) {
        String tradeCountStr = detail.getCount().setScale(scale, mode).toPlainString();
        String categoryName = detail.getCategoryName();

        if (detail.getRemainingCount().compareTo(BigDecimal.ZERO) > 0) {
            String remainingCountStr = detail.getRemainingCount().setScale(scale, mode).toPlainString();

            return String.format("[%s] %s 부분 체결! (%s주 체결되었고, %s주 남았어요.)",
                    categoryName, detail.getType(), tradeCountStr, remainingCountStr);
        } else {

            return String.format("[%s] %s 체결 완료! (총 %s주)",
                    categoryName, detail.getType(), tradeCountStr);
        }
    }
}