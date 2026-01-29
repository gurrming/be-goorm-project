package com.example.heartbit.service;

import com.example.heartbit.domain.Member;
import com.example.heartbit.domain.Notification;
import com.example.heartbit.domain.NotificationType;
import com.example.heartbit.dto.NotificationResponseDto;
import com.example.heartbit.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate; // WebSocket 전송 도구

    // 알림 후 실시간 전송
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 새로운 트랜잭션에서 실행
    public void send(Member member, String content, NotificationType type) {

        Notification notification = new Notification(member, content, type);
        notificationRepository.save(notification);

        // 웹소켓으로 바로 전송 (/topic/notification/{memberId})
        NotificationResponseDto response = NotificationResponseDto.from(notification);
        messagingTemplate.convertAndSend("/topic/notification/" + member.getMemberId(), response);
    }

    // 내 알림 목록 보기
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getNotifications(Long memberId) {
        return notificationRepository.findNotificationsByMember_MemberId(memberId)
                .stream()
                .map(NotificationResponseDto::from)
                .collect(Collectors.toList());
    }

    // 알림 읽기
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
}