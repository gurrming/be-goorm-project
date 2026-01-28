package com.example.heartbit.service;

import com.example.heartbit.domain.Member;
import com.example.heartbit.domain.Notification;
import com.example.heartbit.domain.NotificationType;
import com.example.heartbit.dto.NotificationResponseDto;
import com.example.heartbit.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .memberId(1L)
                .memberEmail("test@test.com")
                .memberNickname("테스터")
                .build();
    }

    @Test
    @DisplayName("알림 생성 및 웹소켓 전송 테스트")
    void send_Success() {
        // given
        String content = "체결이 완료되었습니다.";
        NotificationType type = NotificationType.TRADE;

        // when
        notificationService.send(member, content, type);

        // then
        // 1. DB 저장 확인
        verify(notificationRepository).save(any(Notification.class));
        // 2. 웹소켓 전송 확인 (경로 및 페이로드)
        verify(messagingTemplate).convertAndSend(
                eq("/topic/notification/" + member.getMemberId()),
                any(NotificationResponseDto.class)
        );
    }

    @Test
    @DisplayName("회원 ID로 알림 목록 조회 테스트")
    void getNotifications_Success() {
        // given
        Notification notification = new Notification(member, "알림1", NotificationType.SYSTEM);
        given(notificationRepository.findNotificationsByMember_MemberId(member.getMemberId()))
                .willReturn(List.of(notification));

        // when
        List<NotificationResponseDto> result = notificationService.getNotifications(member.getMemberId());

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNotificationContent()).isEqualTo("알림1");
        assertThat(result.get(0).getNotificationType()).isEqualTo(NotificationType.SYSTEM.name());
    }

    @Test
    @DisplayName("알림 읽음 처리 테스트")
    void markAsRead_Success() {
        // given
        Notification notification = new Notification(member, "알림", NotificationType.TRADE);
        given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

        // when
        notificationService.markAsRead(1L);

        // then
        assertThat(notification.isNotificationIsRead()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 알림 읽음 처리 시 예외 발생")
    void markAsRead_Fail_NotFound() {
        // given
        given(notificationRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            notificationService.markAsRead(1L);
        });
    }
}