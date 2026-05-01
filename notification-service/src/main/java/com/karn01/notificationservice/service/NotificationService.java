package com.karn01.notificationservice.service;

import com.karn01.notificationservice.dto.NotificationResponse;
import com.karn01.notificationservice.entity.Notification;
import com.karn01.notificationservice.event.OrderLifecycleEvent;
import com.karn01.notificationservice.event.PaymentStatusEvent;
import com.karn01.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(notification -> new NotificationResponse(notification.getId(), notification.getTitle(), notification.getMessage(), notification.getType(), notification.getReferenceId(), notification.isRead(), notification.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void markRead(String userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (notification.getUserId().equals(userId)) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    @KafkaListener(topics = "${app.kafka.topics.order-lifecycle}", groupId = "${spring.application.name}", containerFactory = "orderLifecycleKafkaListenerContainerFactory")
    public void handleOrderLifecycle(OrderLifecycleEvent event) {
        Notification notification = new Notification();
        notification.setUserId(event.userId());
        notification.setType("ORDER_" + event.status());
        notification.setReferenceId(event.orderId().toString());
        notification.setRead(false);
        notification.setTitle(buildOrderTitle(event.status()));
        notification.setMessage(event.message());
        notificationRepository.save(notification);
    }

    @Transactional
    @KafkaListener(topics = "${app.kafka.topics.payment-status}", groupId = "${spring.application.name}", containerFactory = "paymentStatusKafkaListenerContainerFactory")
    public void handlePaymentStatus(PaymentStatusEvent event) {
        Notification notification = new Notification();
        notification.setUserId(event.userId());
        notification.setType("PAYMENT_" + event.status());
        notification.setReferenceId(event.paymentId().toString());
        notification.setRead(false);
        notification.setTitle(buildPaymentTitle(event.status()));
        notification.setMessage(buildPaymentMessage(event));
        notificationRepository.save(notification);
    }

    private String buildOrderTitle(String status) {
        return switch (status) {
            case "PENDING_PAYMENT" -> "Order placed";
            case "PAYMENT_COMPLETED" -> "Payment received";
            case "CONFIRMED" -> "Order confirmed";
            case "REJECTED" -> "Order rejected";
            case "REFUNDED" -> "Refund initiated";
            default -> "Order update";
        };
    }

    private String buildPaymentTitle(String status) {
        return switch (status) {
            case "COMPLETED" -> "Payment successful";
            case "FAILED" -> "Payment failed";
            case "REFUNDED" -> "Payment refunded";
            default -> "Payment update";
        };
    }

    private String buildPaymentMessage(PaymentStatusEvent event) {
        return switch (event.status()) {
            case "COMPLETED" -> "Payment for order " + event.orderId() + " was completed successfully.";
            case "FAILED" -> "Payment for order " + event.orderId() + " failed: " + event.reason();
            case "REFUNDED" -> "Payment for order " + event.orderId() + " was refunded: " + event.reason();
            default -> "Payment status updated for order " + event.orderId();
        };
    }
}
