package com.karn01.inventoryservice.repository;

import com.karn01.inventoryservice.entity.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {
    Optional<InventoryReservation> findByOrderId(UUID orderId);
}
