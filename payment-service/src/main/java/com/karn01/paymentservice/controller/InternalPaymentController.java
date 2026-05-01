package com.karn01.paymentservice.controller;

import com.karn01.paymentservice.dto.PaymentInitiationRequest;
import com.karn01.paymentservice.dto.PaymentResponse;
import com.karn01.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/payments/internal")
@RequiredArgsConstructor
public class InternalPaymentController {
    private final PaymentService paymentService;

    @PostMapping
    public PaymentResponse initiate(@Valid @RequestBody PaymentInitiationRequest request) {
        return paymentService.initiate(request);
    }

    @PostMapping("/refund/order/{orderId}")
    public PaymentResponse refund(@PathVariable UUID orderId,
                                  @RequestParam(defaultValue = "Refund initiated by order workflow") String reason) {
        return paymentService.refundByOrderId(orderId, reason);
    }
}
