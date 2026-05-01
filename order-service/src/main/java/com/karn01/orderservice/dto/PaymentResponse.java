package com.karn01.orderservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        String userId,
        BigDecimal amount,
        String paymentMethod,
        String status,
        String gatewayReference,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
