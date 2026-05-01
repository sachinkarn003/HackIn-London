package com.karn01.notificationservice.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderLifecycleEvent(
        UUID orderId,
        String userId,
        String status,
        String message,
        BigDecimal totalAmount,
        LocalDateTime occurredAt
) {
}
