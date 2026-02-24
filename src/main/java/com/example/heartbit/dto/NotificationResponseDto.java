package com.example.heartbit.dto;

import com.example.heartbit.domain.Notification;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationResponseDto {

    private Long notificationId;
    private Long memberId;
    private String notificationContent;
    private String notificationType;
    private boolean notificationIsRead;
    private LocalDateTime createdAt;

    public static NotificationResponseDto from(Notification notification) {
        return NotificationResponseDto.builder()
                .notificationId(notification.getNotificationId())
                .memberId(notification.getMember().getMemberId())
                .notificationContent(notification.getNotificationContent())
                .notificationType(notification.getNotificationType().name())
                .notificationIsRead(notification.isNotificationIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}