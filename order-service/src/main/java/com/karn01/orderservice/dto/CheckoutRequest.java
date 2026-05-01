package com.karn01.orderservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CheckoutRequest(
        @NotBlank(message = "Shipping address is required")
        @Size(max = 255, message = "Shipping address must be at most 255 characters")
        String shippingAddress,
        @NotBlank(message = "City is required")
        String city,
        @NotBlank(message = "State is required")
        String state,
        @NotBlank(message = "Postal code is required")
        String postalCode,
        @NotBlank(message = "Country is required")
        String country,
        @NotBlank(message = "Payment method is required")
        String paymentMethod
) {
}
