package com.karn01.paymentservice.dto;

import com.karn01.paymentservice.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        String userId,
        BigDecimal amount,
        String paymentMethod,
        PaymentStatus status,
        String gatewayReference,
        String razorpayOrderId,
        String razorpayPaymentId,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
