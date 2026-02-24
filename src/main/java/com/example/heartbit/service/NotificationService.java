package com.example.heartbit.service;

import com.example.heartbit.domain.Member;
import com.example.heartbit.domain.Notification;
import com.example.heartbit.domain.NotificationType;
import com.example.heartbit.dto.NotificationResponseDto;
import com.example.heartbit.repository.InterestRepository;
import com.example.heartbit.repository.InvestRepository;
import com.example.heartbit.repository.NotificationRepository;
import com.example.heartbit.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest; // Pageable 구현체
import org.springframework.data.domain.Pageable;    // Spring Data 용
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final InterestRepository interestRepository;
    private final InvestRepository investRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(Member member, String content, NotificationType type) {
        Notification notification = new Notification(member, content, type);
        notificationRepository.save(notification);

        NotificationResponseDto response = NotificationResponseDto.from(notification);
        messagingTemplate.convertAndSend("/topic/notification/" + member.getMemberId(), response);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getNotifications(Long memberId, Long lastNotiId, int size) {

        Pageable pageable = PageRequest.of(0, size);

        // 3일 전
        LocalDateTime limitDate = LocalDateTime.now().minusDays(3);

        List<Notification> notifications;

        if (lastNotiId == null) {
            // 처음 불러올 떄
            notifications = notificationRepository.findLatestNotifications(memberId, limitDate, pageable);
        } else {
            // 다음거 가져오기
            notifications = notificationRepository.findOlderNotifications(memberId, lastNotiId, limitDate, pageable);
        }

        return notifications.stream()
                .map(NotificationResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 알림입니다."));
        notification.read();
    }

    @Transactional
    public void markAllAsRead(Long memberId) {
        List<Notification> notifications =
                notificationRepository.findAllByMember_MemberIdAndNotificationIsReadFalse(memberId);
        notifications.forEach(Notification::read);
    }

    private final Map<String, Integer> lastNotifiedStep = new ConcurrentHashMap<>();

    public void clearNotificationHistory() {
        lastNotifiedStep.clear();
    }

    @Async
    @Transactional
    public void checkAndSendPriceAlert(Long categoryId, BigDecimal currentPrice, String categoryName, BigDecimal referencePrice) {
        // 방어 로직
        if (referencePrice == null || referencePrice.compareTo(BigDecimal.ZERO) <= 0 || currentPrice == null) {
            return;
        }

        try {
            // 1. 변동률 계산
            BigDecimal rate = currentPrice.subtract(referencePrice)
                    .divide(referencePrice, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));

            double rawRate = rate.doubleValue();
            int currentStep;

            // 2. 5% 단위 구간 계산 (상승/하락 모두 포함)
            if (Math.abs(rawRate) >= 5.0) {
                // rawRate가 -7.5라면 (int)(-7.5/5)*5 = -5가 됨
                currentStep = ((int) (rawRate / 5)) * 5;
            } else {
                return;
            }

            // 3. 중복 체크 키 (종목ID:구간값)
            String key = categoryId + ":" + currentStep;

            if (lastNotifiedStep.putIfAbsent(key, currentStep) == null) {

                // 상승/하락 문구 결정
                String direction = currentStep > 0 ? "상승" : "하락";
                int displayPercentage = Math.abs(currentStep);

                String commonMsg = String.format("[%s] 전일 대비 %d%% %s 중", categoryName, displayPercentage, direction);

                // 관심 종목 알림
                interestRepository.findByCategory_CategoryId(categoryId).forEach(i ->
                        send(i.getMember(), commonMsg, NotificationType.INTEREST)
                );

                // 보유 종목 알림 (ASSET 타입)
                investRepository.findByCategory_CategoryId(categoryId).forEach(inv ->
                        send(inv.getMember(), commonMsg, NotificationType.ASSET)
                );
            }
        } catch (Exception e) {
            // 비동기 에러 방어용 (로그 생략 요청 반영)
        }
    }

}