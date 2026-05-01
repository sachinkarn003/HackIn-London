package com.karn01.paymentservice.dto;

import jakarta.validation.constraints.NotBlank;

public record RazorpayVerifyRequest(
        @NotBlank String razorpayOrderId,
        @NotBlank String razorpayPaymentId,
        @NotBlank String razorpaySignature
) {
}
