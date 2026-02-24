package com.example.heartbit.repository;

import com.example.heartbit.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 알림목록박스 첫 진입
    @Query("""
        SELECT n FROM Notification n 
        WHERE n.member.memberId = :memberId 
          AND n.createdAt >= :limitDate
        ORDER BY n.notificationId DESC
    """)
    List<Notification> findLatestNotifications(
            @Param("memberId") Long memberId,
            @Param("limitDate") LocalDateTime limitDate,
            Pageable pageable
    );

    // 이어서 불러오기
    @Query("""
        SELECT n FROM Notification n 
        WHERE n.member.memberId = :memberId 
          AND n.notificationId < :lastNotificationId 
          AND n.createdAt >= :limitDate
        ORDER BY n.notificationId DESC
    """)
    List<Notification> findOlderNotifications(
            @Param("memberId") Long memberId,
            @Param("lastNotificationId") Long lastNotificationId,
            @Param("limitDate") LocalDateTime limitDate,
            Pageable pageable
    );

    // 안읽은 특정 멤버 알림목록 전부 가져오기
    List<Notification> findAllByMember_MemberIdAndNotificationIsReadFalse(Long memberId);

}