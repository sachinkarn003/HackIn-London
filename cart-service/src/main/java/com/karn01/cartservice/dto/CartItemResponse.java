package com.karn01.cartservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        UUID variantId,
        Integer quantity,
        BigDecimal price,
        String productName,
        BigDecimal total
) {
}
