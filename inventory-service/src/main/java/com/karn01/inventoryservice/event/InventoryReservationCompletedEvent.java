package com.karn01.inventoryservice.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryReservationCompletedEvent(
        UUID orderId,
        String userId,
        boolean success,
        String reason,
        LocalDateTime processedAt
) {
}
