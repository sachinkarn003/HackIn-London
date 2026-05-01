package com.karn01.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentInitiationRequest(
        @NotNull UUID orderId,
        @NotBlank String userId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String paymentMethod
) {
}
