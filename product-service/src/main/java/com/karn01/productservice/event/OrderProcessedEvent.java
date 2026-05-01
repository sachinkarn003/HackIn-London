package com.karn01.productservice.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderProcessedEvent(
        UUID orderId,
        String status,
        String reason,
        LocalDateTime processedAt
) {
}
