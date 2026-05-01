package com.karn01.orderservice.event;

import com.karn01.orderservice.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderProcessedEvent(UUID orderId, OrderStatus status, String reason, LocalDateTime processedAt) {
}
