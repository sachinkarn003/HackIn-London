package com.karn01.productservice.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        String userId,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        List<OrderItemEvent> items
) {
}
