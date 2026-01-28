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




}