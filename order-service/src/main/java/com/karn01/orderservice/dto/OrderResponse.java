package com.karn01.orderservice.dto;

import com.karn01.orderservice.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String userId,
        OrderStatus status,
        BigDecimal totalAmount,
        String shippingAddress,
        String city,
        String state,
        String postalCode,
        String country,
        String paymentMethod,
        UUID paymentId,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrderItemResponse> items
) {
}
