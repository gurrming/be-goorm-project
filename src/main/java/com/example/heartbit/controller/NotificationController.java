package com.example.heartbit.controller;

import com.example.heartbit.dto.NotificationResponseDto;
import com.example.heartbit.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "알림 관련 API", description = "알림 목록 조회, 알림 읽기 등을 담당합니다.")
public class NotificationController {

    private final NotificationService notificationService;

    // 내 알림 목록 조회
    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>> getMyNotifications(@RequestParam Long memberId) {
        return ResponseEntity.ok(notificationService.getNotifications(memberId));
    }

    // 알림 읽음 처리
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }

    // 알림 전체 읽음처리
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@RequestParam Long memberId) {
        notificationService.markAllAsRead(memberId);
        return ResponseEntity.ok().build();
    }
}