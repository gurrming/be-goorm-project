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
    private final TradeRepository tradeRepository;
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

//    @Async
//    @Transactional(readOnly = true)
//    public void checkAndSendPriceAlert(Long categoryId, BigDecimal currentPrice, String symbol) {
//        try {
//            log.info("시세 알림 체크 시작 - 종목: {}, 현재가: {}", symbol, currentPrice);
//
//            BigDecimal yesterdayClose = tradeRepository.findYesterdayClosePrice(categoryId).orElse(null);
//            log.info("전일 종가 조회 결과: {}", yesterdayClose);
//
//            if (yesterdayClose == null || yesterdayClose.compareTo(BigDecimal.ZERO) <= 0) return;
//            // 2. 변동률 계산
//            BigDecimal rate = currentPrice.subtract(yesterdayClose)
//                    .divide(yesterdayClose, 4, RoundingMode.HALF_UP)
//                    .multiply(new BigDecimal("100"));
//
//            // 3. 5% 단위 구간 계산 (예: 5.3% -> 5, 11% -> 10)
//            int currentStep = rate.divide(new BigDecimal("5"), 0, RoundingMode.FLOOR).intValue() * 5;
//
//            if (currentStep >= 5) {
//                String key = categoryId + ":" + currentStep;
//
//                // 4. 해당 구간에 처음 진입했을 때만 실행
//                if (lastNotifiedStep.putIfAbsent(key, currentStep) == null) {
//                    // 관심 종목 사용자에게 기존 send 활용
//                    interestRepository.findByCategory_CategoryId(categoryId).forEach(i ->
//                            send(i.getMember(), String.format("관심 종목 [%s], 전일 대비 %d%% 돌파!", symbol, currentStep), NotificationType.INTEREST));
//
//                    // 보유 종목 사용자에게 기존 send 활용
//                    investRepository.findByCategory_CategoryId(categoryId).forEach(inv ->
//                            send(inv.getMember(), String.format("보유하신 [%s] %d%% 상승 중!", symbol, currentStep), NotificationType.TRADE));
//                }
//            }
//        } catch (Exception e) {
//
//        }
//    }


    @Async
    @Transactional(readOnly = true)
    public void checkAndSendPriceAlert(Long categoryId, BigDecimal currentPrice, String categoryName) {
        log.info("[알림체크 시작] 종목ID: {}, 종목명: {}, 현재가: {}", categoryId, categoryName, currentPrice);
        try {
            // 1. 전일 종가 조회 로그
            BigDecimal yesterdayClose = tradeRepository.findYesterdayClosePrice(categoryId).orElse(null);
            log.info("[1. 전일종가조회] 결과: {}", yesterdayClose);

            if (yesterdayClose == null || yesterdayClose.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("[중단] 전일 종가가 없거나 0원입니다. (종목: {})", categoryName);
                return;
            }

            // 2. 변동률 계산 로직
            BigDecimal rate = currentPrice.subtract(yesterdayClose)
                    .divide(yesterdayClose, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));

            // 3. 5% 단위 구간 계산
            int currentStep = rate.divide(new BigDecimal("5"), 0, RoundingMode.FLOOR).intValue() * 5;
            log.info("[2. 계산결과] 변동률: {}%, 판별된구간: {}%", rate, currentStep);

            if (currentStep >= 5) {
                String key = categoryId + ":" + currentStep;

                if (lastNotifiedStep.putIfAbsent(key, currentStep) == null) {
                    log.info("[3. 알림발송진입] 새로운 구간 {}% 진입 (Key: {})", currentStep, key);

                    // 4. 관심 종목 알림 루프
                    interestRepository.findByCategory_CategoryId(categoryId).forEach(i -> {
                        try {
                            log.info("[관심알림 시도] 사용자ID: {}, 종목: {}", i.getMember().getMemberId(), categoryName);
                            send(i.getMember(),
                                    String.format("관심 종목 [%s], 전일 대비 %d%% 돌파!", categoryName, currentStep),
                                    NotificationType.INTEREST);
                        } catch (Exception e) {
                            log.error("[관심알림 실패] 사용자ID: {}, 에러: {}", i.getMember().getMemberId(), e.getMessage());
                        }
                    });

                    // 5. 보유 종목 알림 루프
                    investRepository.findByCategory_CategoryId(categoryId).forEach(inv -> {
                        try {
                            log.info("[보유알림 시도] 사용자ID: {}, 종목: {}", inv.getMember().getMemberId(), categoryName);
                            send(inv.getMember(),
                                    String.format("보유하신 [%s] %d%% 상승 중!", categoryName, currentStep),
                                    NotificationType.TRADE);
                        } catch (Exception e) {
                            log.error("[보유알림 실패] 사용자ID: {}, 에러: {}", inv.getMember().getMemberId(), e.getMessage());
                        }
                    });
                } else {
                    log.info("[알림건너뜀] 이미 오늘 {}% 구간 알림을 보냈습니다.", currentStep);
                }
            }
        } catch (Exception e) {
            log.error("[최종오류] checkAndSendPriceAlert 메서드 실행 중 예외 발생!", e);
        }
    }

}