package com.karn01.productservice.event;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemEvent(
        UUID variantId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice
) {
}
