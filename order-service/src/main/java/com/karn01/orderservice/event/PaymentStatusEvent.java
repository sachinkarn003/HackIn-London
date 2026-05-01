package com.karn01.orderservice.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentStatusEvent(
        UUID paymentId,
        UUID orderId,
        String userId,
        BigDecimal amount,
        String paymentMethod,
        String status,
        String reason,
        LocalDateTime occurredAt
) {
}
