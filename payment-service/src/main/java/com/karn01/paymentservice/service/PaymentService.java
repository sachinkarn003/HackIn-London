package com.karn01.paymentservice.service;

import com.karn01.paymentservice.dto.PaymentActionRequest;
import com.karn01.paymentservice.dto.PaymentInitiationRequest;
import com.karn01.paymentservice.dto.PaymentResponse;
import com.karn01.paymentservice.dto.RazorpayConfigResponse;
import com.karn01.paymentservice.dto.RazorpayVerifyRequest;
import com.karn01.paymentservice.entity.Payment;
import com.karn01.paymentservice.entity.PaymentStatus;
import com.karn01.paymentservice.event.PaymentStatusEvent;
import com.karn01.paymentservice.exception.BadRequestException;
import com.karn01.paymentservice.exception.ResourceNotFoundException;
import com.karn01.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, PaymentStatusEvent> kafkaTemplate;

    @Value("${app.kafka.topics.payment-status}")
    private String paymentStatusTopic;

    @Value("${app.razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${app.razorpay.key-secret:}")
    private String razorpayKeySecret;

    @Value("${app.razorpay.currency:INR}")
    private String razorpayCurrency;

    @Value("${app.razorpay.merchant-name:MEN.}")
    private String merchantName;

    @Transactional
    public PaymentResponse initiate(PaymentInitiationRequest request) {
        Payment payment = paymentRepository.findByOrderId(request.orderId()).orElseGet(Payment::new);
        payment.setOrderId(request.orderId());
        payment.setUserId(request.userId());
        payment.setAmount(request.amount());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setStatus(PaymentStatus.PENDING);
        if (payment.getGatewayReference() == null) {
            if (isOnlinePayment(request.paymentMethod())) {
                String razorpayOrderId = createRazorpayOrder(request);
                payment.setGatewayReference(razorpayOrderId);
                payment.setRazorpayOrderId(razorpayOrderId);
            } else {
                payment.setGatewayReference("PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
            }
        }
        payment.setFailureReason(null);
        return toResponse(paymentRepository.save(payment));
    }

    public RazorpayConfigResponse getRazorpayConfig() {
        if (razorpayKeyId == null || razorpayKeyId.isBlank()) {
            throw new BadRequestException("Razorpay key id is not configured");
        }
        return new RazorpayConfigResponse(razorpayKeyId, razorpayCurrency, merchantName);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForUser(String userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentForUser(String userId, UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (!payment.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Payment not found");
        }
        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse simulateSuccess(String userId, UUID paymentId) {
        Payment payment = getOwnedPayment(userId, paymentId);
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Only pending payments can be completed");
        }
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setFailureReason(null);
        Payment saved = paymentRepository.save(payment);
        publishStatus(saved, null);
        return toResponse(saved);
    }

    @Transactional
    public PaymentResponse simulateFailure(String userId, UUID paymentId, PaymentActionRequest request) {
        Payment payment = getOwnedPayment(userId, paymentId);
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Only pending payments can be failed");
        }
        String reason = request == null || request.reason() == null || request.reason().isBlank()
                ? "Payment authorization declined"
                : request.reason();
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(reason);
        Payment saved = paymentRepository.save(payment);
        publishStatus(saved, reason);
        return toResponse(saved);
    }

    @Transactional
    public PaymentResponse verifyRazorpayPayment(String userId, UUID paymentId, RazorpayVerifyRequest request) {
        Payment payment = getOwnedPayment(userId, paymentId);
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Only pending payments can be verified");
        }
        if (payment.getRazorpayOrderId() == null || !payment.getRazorpayOrderId().equals(request.razorpayOrderId())) {
            throw new BadRequestException("Razorpay order mismatch");
        }
        if (!isValidRazorpaySignature(request)) {
            throw new BadRequestException("Invalid Razorpay signature");
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setRazorpayPaymentId(request.razorpayPaymentId());
        payment.setFailureReason(null);
        Payment saved = paymentRepository.save(payment);
        publishStatus(saved, null);
        return toResponse(saved);
    }

    @Transactional
    public PaymentResponse refundByOrderId(UUID orderId, String reason) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return toResponse(payment);
        }
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BadRequestException("Only completed payments can be refunded");
        }
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setFailureReason(reason);
        Payment saved = paymentRepository.save(payment);
        publishStatus(saved, reason);
        return toResponse(saved);
    }

    private Payment getOwnedPayment(String userId, UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (!payment.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Payment not found");
        }
        return payment;
    }

    private void publishStatus(Payment payment, String reason) {
        kafkaTemplate.send(paymentStatusTopic, payment.getOrderId().toString(), new PaymentStatusEvent(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                reason,
                LocalDateTime.now()
        ));
    }

    private boolean isOnlinePayment(String paymentMethod) {
        return "CARD".equalsIgnoreCase(paymentMethod) || "UPI".equalsIgnoreCase(paymentMethod);
    }

    private String createRazorpayOrder(PaymentInitiationRequest request) {
        if (razorpayKeyId == null || razorpayKeyId.isBlank() || razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            throw new BadRequestException("Razorpay credentials are not configured");
        }

        long amountInPaise = request.amount().multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(razorpayKeyId, razorpayKeySecret, StandardCharsets.UTF_8);

        Map<String, Object> body = Map.of(
                "amount", amountInPaise,
                "currency", razorpayCurrency,
                "receipt", request.orderId().toString(),
                "notes", Map.of("paymentId", request.orderId().toString(), "userId", request.userId())
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = new RestTemplate().postForObject(
                "https://api.razorpay.com/v1/orders",
                new HttpEntity<>(body, headers),
                Map.class
        );
        if (response == null || response.get("id") == null) {
            throw new BadRequestException("Razorpay order could not be created");
        }
        return response.get("id").toString();
    }

    private boolean isValidRazorpaySignature(RazorpayVerifyRequest request) {
        if (razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            throw new BadRequestException("Razorpay secret is not configured");
        }
        try {
            String payload = request.razorpayOrderId() + "|" + request.razorpayPaymentId();
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = bytesToHex(digest);
            return MessageDigestSupport.constantTimeEquals(expected, request.razorpaySignature());
        } catch (Exception ex) {
            throw new BadRequestException("Unable to verify Razorpay signature");
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getGatewayReference(),
                payment.getRazorpayOrderId(),
                payment.getRazorpayPaymentId(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    private static final class MessageDigestSupport {
        private static boolean constantTimeEquals(String expected, String actual) {
            if (actual == null) {
                return false;
            }
            byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
            byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
            if (expectedBytes.length != actualBytes.length) {
                return false;
            }
            int result = 0;
            for (int index = 0; index < expectedBytes.length; index++) {
                result |= expectedBytes[index] ^ actualBytes[index];
            }
            return result == 0;
        }
    }
}
