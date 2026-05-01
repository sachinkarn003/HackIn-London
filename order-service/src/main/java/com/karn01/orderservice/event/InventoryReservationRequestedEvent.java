package com.karn01.orderservice.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record InventoryReservationRequestedEvent(
        UUID orderId,
        String userId,
        LocalDateTime requestedAt,
        List<OrderItemEvent> items
) {
}
