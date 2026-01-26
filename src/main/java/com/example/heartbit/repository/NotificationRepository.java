package com.example.heartbit.repository;

import com.example.heartbit.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 특정 멤버 알림 목록 조회
    List<Notification> findNotificationsByMemberId(Long memberId);

}