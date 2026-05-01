package com.karn01.inventoryservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record VariantDetailDto(UUID id, String productName, BigDecimal price, Integer stock) {
}
