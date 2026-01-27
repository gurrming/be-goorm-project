package com.example.heartbit.integration;

import com.example.heartbit.domain.Member;
import com.example.heartbit.domain.NotificationType;
import com.example.heartbit.dto.NotificationResponseDto;
import com.example.heartbit.repository.MemberRepository;
import com.example.heartbit.repository.NotificationRepository;
import com.example.heartbit.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class NotificationIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private MemberRepository memberRepository;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    private Member testMember;

    @BeforeEach
    void setUp() {
        // 테스트용 멤버 생성
        testMember = memberRepository.save(Member.builder()
                .memberEmail("test@naver.com")
                .memberNickname("구르밍")
                .memberPassword("1111")
                .build());
    }

    @Test
    @DisplayName("체결 완료 시 알림 저장 및 전송 테스트")
    void sendNotificationTest() {
        // given: 체결 완료
        String content = "[비트코인] 매수 체결 완료!";
        NotificationType type = NotificationType.TRADE;

        // when: 알림 전송
        notificationService.send(testMember, content, type);

        // then: DB에 알림이 저장되었는지 확인
        List<NotificationResponseDto> result = notificationService.getNotifications(testMember.getMemberId());

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getNotificationContent()).isEqualTo(content);
    }

    @Test
    @DisplayName("특정 멤버의 알림 목록 조회 테스트")
    void getNotificationsTest() {
        // given: 알림 생성
        notificationService.send(testMember, "[비트코인] 매수 체결 완료!", NotificationType.TRADE);
        notificationService.send(testMember, "[이더리움] 매도 체결 완료!", NotificationType.TRADE);

        // when: 해당 멤버의 알림 목록 조회
        List<NotificationResponseDto> notifications = notificationService.getNotifications(testMember.getMemberId());

        // then: 목록의 개수 및 내용 검증
        assertThat(notifications).hasSize(2);
        assertThat(notifications)
                .extracting(NotificationResponseDto::getNotificationContent)
                .containsExactlyInAnyOrder("[비트코인] 매수 체결 완료!", "[이더리움] 매도 체결 완료!");
    }
}