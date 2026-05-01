package com.karn01.paymentservice.repository;

import com.karn01.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByOrderId(UUID orderId);
    List<Payment> findByUserIdOrderByCreatedAtDesc(String userId);
}
