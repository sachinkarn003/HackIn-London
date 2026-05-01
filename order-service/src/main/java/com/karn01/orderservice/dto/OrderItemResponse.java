package com.karn01.orderservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(UUID variantId, String productName, Integer quantity, BigDecimal unitPrice, BigDecimal subtotal) {
}
