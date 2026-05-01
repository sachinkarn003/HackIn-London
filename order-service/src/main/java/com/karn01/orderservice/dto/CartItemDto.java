package com.karn01.orderservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemDto(UUID variantId, Integer quantity, BigDecimal price, String productName, BigDecimal total) {
}
