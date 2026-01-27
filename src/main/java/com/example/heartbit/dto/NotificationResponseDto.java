package com.example.heartbit.dto;

import com.example.heartbit.domain.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponseDto {

    private Long notificationId;
    private String notificationContent;
    private String notificationType;
    private boolean notificationIsRead;
    private LocalDateTime createdAt;

    public static NotificationResponseDto from(Notification notification) {
        return NotificationResponseDto.builder()
                .notificationId(notification.getNotificationId())
                .notificationContent(notification.getNotificationContent())
                .notificationType(notification.getNotificationType().name()) // Enum -> String
                .notificationIsRead(notification.isNotificationIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}