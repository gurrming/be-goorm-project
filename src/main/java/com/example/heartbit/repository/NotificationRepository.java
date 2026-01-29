package com.example.heartbit.repository;

import com.example.heartbit.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query(value = """
        SELECT * FROM notification n 
        WHERE n.member_id = :memberId 
          AND n.created_at >= NOW() - INTERVAL '1 day'
    """, nativeQuery = true)
    List<Notification> findNotificationsByMember_MemberId(@Param("memberId") Long memberId);
    List<Notification> findAllByMember_MemberIdAndNotificationIsReadFalse(Long memberId);

}