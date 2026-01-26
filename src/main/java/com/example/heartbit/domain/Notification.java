package com.example.heartbit.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "notification_content", length = 200)
    private String notificationContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Column(name = "notification_is_read", nullable = false)
    private boolean notificationIsRead;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Notification(Member member, String notificationContent, NotificationType notificationType){
        this.member = member;
        this.notificationContent = notificationContent;
        this.notificationType = notificationType;
        this.notificationIsRead = false;
        this.createdAt = LocalDateTime.now();
    }

    public void read() {
        this.notificationIsRead = true;
    }

}
