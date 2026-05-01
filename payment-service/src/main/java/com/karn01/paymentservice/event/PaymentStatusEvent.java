package com.karn01.paymentservice.event;

import com.karn01.paymentservice.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentStatusEvent(
        UUID paymentId,
        UUID orderId,
        String userId,
        BigDecimal amount,
        String paymentMethod,
        PaymentStatus status,
        String reason,
        LocalDateTime occurredAt
) {
}
