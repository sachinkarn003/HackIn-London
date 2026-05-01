package com.karn01.productservice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StockAdjustmentRequest(@NotNull UUID variantId, int quantityDelta) {
}
