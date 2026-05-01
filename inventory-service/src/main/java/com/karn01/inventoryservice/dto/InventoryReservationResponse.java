package com.karn01.inventoryservice.dto;

import com.karn01.inventoryservice.entity.ReservationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record InventoryReservationResponse(
        UUID id,
        UUID orderId,
        String userId,
        ReservationStatus status,
        String reason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<InventoryReservationItemResponse> items
) {
}
