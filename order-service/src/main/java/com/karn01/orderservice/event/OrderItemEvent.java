package com.karn01.orderservice.event;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemEvent(UUID variantId, String productName, Integer quantity, BigDecimal unitPrice) {
}
