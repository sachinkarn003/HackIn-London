package com.karn01.inventoryservice.dto;

import java.util.UUID;

public record InventoryReservationItemResponse(UUID variantId, String productName, int quantity) {
}
