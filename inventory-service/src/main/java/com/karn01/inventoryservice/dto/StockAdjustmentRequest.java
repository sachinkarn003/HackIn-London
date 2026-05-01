package com.karn01.inventoryservice.dto;

import java.util.UUID;

public record StockAdjustmentRequest(UUID variantId, int quantityDelta) {
}
