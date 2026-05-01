package com.karn01.notificationservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String title,
        String message,
        String type,
        String referenceId,
        boolean isRead,
        LocalDateTime createdAt
) {
}
