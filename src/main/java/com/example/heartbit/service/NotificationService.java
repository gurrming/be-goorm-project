package com.example.heartbit.service;

import com.example.heartbit.domain.Member;
import com.example.heartbit.domain.Notification;
import com.example.heartbit.domain.NotificationType;
import com.example.heartbit.dto.NotificationResponseDto;
import com.example.heartbit.repository.InterestRepository;
import com.example.heartbit.repository.InvestRepository;
import com.example.heartbit.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
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

    private final Map<String, Integer> lastNotifiedStep = new ConcurrentHashMap<>();

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(Member member, String content, NotificationType type) {
        Notification notification = new Notification(member, content, type);
        notificationRepository.save(notification);

        NotificationResponseDto response = NotificationResponseDto.from(notification);
        messagingTemplate.convertAndSend("/topic/notification/" + member.getMemberId(), response);
    }

    /**
     * 시세 변동 알림 객체 생성 (Batch 처리용)
     * 필터링: 동일 종목에 대해 보유(ASSET)와 관심(INTEREST)이 겹치면 ASSET 알림만 생성합니다.
     */
    @Transactional(readOnly = true)
    public List<Notification> createPriceAlerts(Long categoryId, BigDecimal currentPrice, String categoryName, BigDecimal referencePrice) {
        List<Notification> alerts = new ArrayList<>();

        log.info("[입력값 확인] 종목: {}, 현재가: {}, 기준가: {}", categoryName, currentPrice, referencePrice);

        if (referencePrice == null || referencePrice.compareTo(BigDecimal.ZERO) <= 0 || currentPrice == null) {

            log.warn("[시세체크 중단] 기준가 혹은 현재가가 유효하지 않음. 기준가: {}", referencePrice);
            return alerts;
        }

        try {
            // 1. 변동률 및 5% 구간 계산
            BigDecimal rate = currentPrice.subtract(referencePrice)
                    .divide(referencePrice, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));

            log.info("[시세체크 A] 종목: {}, 변동률: {}%, 현재가: {}, 기준가: {}", categoryName, rate, currentPrice, referencePrice);

            double rawRate = rate.doubleValue();
            if (Math.abs(rawRate) < 5.0) return alerts;

            log.info("[시세체크 B] 5% 조건 통과! rawRate: {}", rawRate);

            int currentStep = ((int) (rawRate / 5)) * 5;
            String key = categoryId + ":" + currentStep;

            // 2. 중복 알림 방지 체크 (동일 종목, 동일 구간)
            if (lastNotifiedStep.putIfAbsent(key, currentStep) == null) {
                log.info("[시세체크 C] 중복 아님. 알림 생성 시작. 키: {}", key);
                String direction = currentStep > 0 ? "상승" : "하락";
                int displayPercentage = Math.abs(currentStep);
                String commonMsg = String.format("[%s] 전일 대비 %d%% %s 중", categoryName, displayPercentage, direction);

                Set<Long> investedMemberIds = new HashSet<>();

                // 보유 종목 대상자 알림 생성
                investRepository.findByCategory_CategoryId(categoryId).forEach(inv -> {
                    if (inv.getMember() != null) {
                        Long memberId = inv.getMember().getMemberId();
                        investedMemberIds.add(memberId);
                        alerts.add(new Notification(inv.getMember(), commonMsg, NotificationType.ASSET));
                    }
                });

                // 보유 중이 아닌 경우
                interestRepository.findByCategory_CategoryId(categoryId).forEach(i -> {
                    if (i.getMember() != null) {
                        Long memberId = i.getMember().getMemberId();
                        if (!investedMemberIds.contains(memberId)) {
                            alerts.add(new Notification(i.getMember(), commonMsg, NotificationType.INTEREST));
                        }
                    }
                });
            }
        } catch (Exception e) {
            log.error("[시세 알림 생성 오류] categoryId: {}, 에러: {}", categoryId, e.getMessage());
        }
        return alerts;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getNotifications(Long memberId, Long lastNotiId, int size) {
        Pageable pageable = PageRequest.of(0, size);
        LocalDateTime limitDate = LocalDateTime.now().minusDays(3);
        List<Notification> notifications = (lastNotiId == null) ?
                notificationRepository.findLatestNotifications(memberId, limitDate, pageable) :
                notificationRepository.findOlderNotifications(memberId, lastNotiId, limitDate, pageable);

        return notifications.stream().map(NotificationResponseDto::from).collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(Notification::read);
    }

    @Transactional
    public void markAllAsRead(Long memberId) {
        notificationRepository.findAllByMember_MemberIdAndNotificationIsReadFalse(memberId).forEach(Notification::read);
    }

    public void clearNotificationHistory() {
        lastNotifiedStep.clear();
    }
}