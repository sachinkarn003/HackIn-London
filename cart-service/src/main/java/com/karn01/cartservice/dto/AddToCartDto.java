package com.karn01.cartservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AddToCartDto {
    @NotNull(message = "Variant id is required")
    private UUID variantId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;
}
