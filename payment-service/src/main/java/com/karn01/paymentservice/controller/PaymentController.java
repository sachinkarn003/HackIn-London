package com.karn01.paymentservice.controller;

import com.karn01.paymentservice.dto.ApiResponse;
import com.karn01.paymentservice.dto.PaymentActionRequest;
import com.karn01.paymentservice.dto.PaymentResponse;
import com.karn01.paymentservice.dto.RazorpayConfigResponse;
import com.karn01.paymentservice.dto.RazorpayVerifyRequest;
import com.karn01.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @GetMapping
    public ApiResponse<List<PaymentResponse>> getMyPayments(@RequestHeader("X-User-Id") String userId) {
        return new ApiResponse<>(true, "Payments fetched successfully", paymentService.getPaymentsForUser(userId));
    }

    @GetMapping("/razorpay/config")
    public ApiResponse<RazorpayConfigResponse> getRazorpayConfig() {
        return new ApiResponse<>(true, "Razorpay config fetched successfully", paymentService.getRazorpayConfig());
    }

    @GetMapping("/order/{orderId}")
    public ApiResponse<PaymentResponse> getPaymentByOrder(@RequestHeader("X-User-Id") String userId,
                                                          @PathVariable UUID orderId) {
        return new ApiResponse<>(true, "Payment fetched successfully", paymentService.getPaymentForUser(userId, orderId));
    }

    @PostMapping("/{paymentId}/simulate-success")
    public ApiResponse<PaymentResponse> simulateSuccess(@RequestHeader("X-User-Id") String userId,
                                                        @PathVariable UUID paymentId) {
        return new ApiResponse<>(true, "Payment completed successfully", paymentService.simulateSuccess(userId, paymentId));
    }

    @PostMapping("/{paymentId}/simulate-failure")
    public ApiResponse<PaymentResponse> simulateFailure(@RequestHeader("X-User-Id") String userId,
                                                        @PathVariable UUID paymentId,
                                                        @RequestBody(required = false) PaymentActionRequest request) {
        return new ApiResponse<>(true, "Payment failed successfully", paymentService.simulateFailure(userId, paymentId, request));
    }

    @PostMapping("/{paymentId}/razorpay/verify")
    public ApiResponse<PaymentResponse> verifyRazorpayPayment(@RequestHeader("X-User-Id") String userId,
                                                              @PathVariable UUID paymentId,
                                                              @Valid @RequestBody RazorpayVerifyRequest request) {
        return new ApiResponse<>(true, "Payment verified successfully", paymentService.verifyRazorpayPayment(userId, paymentId, request));
    }
}
