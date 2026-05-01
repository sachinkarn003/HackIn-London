package com.karn01.notificationservice.controller;

import com.karn01.notificationservice.dto.ApiResponse;
import com.karn01.notificationservice.dto.NotificationResponse;
import com.karn01.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<NotificationResponse>> getNotifications(@RequestHeader("X-User-Id") String userId) {
        return new ApiResponse<>(true, "Notifications fetched successfully", notificationService.getNotifications(userId));
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<Void> markRead(@RequestHeader("X-User-Id") String userId,
                                      @PathVariable UUID notificationId) {
        notificationService.markRead(userId, notificationId);
        return new ApiResponse<>(true, "Notification marked as read", null);
    }
}
