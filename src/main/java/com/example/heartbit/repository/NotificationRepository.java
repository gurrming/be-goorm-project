package com.example.heartbit.repository;

import com.example.heartbit.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findNotificationsByMember_MemberId(Long memberId);
}