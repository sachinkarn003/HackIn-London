package com.karn01.inventoryservice.service;

import com.karn01.inventoryservice.dto.InventoryReservationItemResponse;
import com.karn01.inventoryservice.dto.InventoryReservationResponse;
import com.karn01.inventoryservice.dto.StockAdjustmentRequest;
import com.karn01.inventoryservice.entity.InventoryReservation;
import com.karn01.inventoryservice.entity.InventoryReservationItem;
import com.karn01.inventoryservice.entity.ProcessedEventLog;
import com.karn01.inventoryservice.entity.ReservationStatus;
import com.karn01.inventoryservice.event.InventoryReservationCompletedEvent;
import com.karn01.inventoryservice.event.InventoryReservationRequestedEvent;
import com.karn01.inventoryservice.event.OrderItemEvent;
import com.karn01.inventoryservice.exception.ResourceNotFoundException;
import com.karn01.inventoryservice.repository.InventoryReservationRepository;
import com.karn01.inventoryservice.repository.ProcessedEventLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private static final String INVENTORY_RESERVATION_REQUESTED_EVENT = "INVENTORY_RESERVATION_REQUESTED";

    private final InventoryReservationRepository reservationRepository;
    private final ProcessedEventLogRepository processedEventLogRepository;
    private final RestTemplate restTemplate;
    private final KafkaTemplate<String, InventoryReservationCompletedEvent> kafkaTemplate;

    @Value("${app.services.product-service-url}")
    private String productServiceUrl;

    @Value("${app.kafka.topics.inventory-reservation-completed}")
    private String inventoryReservationCompletedTopic;

    @Transactional(readOnly = true)
    public InventoryReservationResponse getReservation(UUID orderId) {
        InventoryReservation reservation = reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory reservation not found"));
        return toResponse(reservation);
    }

    @Transactional
    @KafkaListener(topics = "${app.kafka.topics.inventory-reservation-requested}", groupId = "${spring.application.name}", containerFactory = "inventoryReservationRequestedKafkaListenerContainerFactory")
    public void reserveInventory(InventoryReservationRequestedEvent event) {
        if (processedEventLogRepository.existsByAggregateIdAndEventType(event.orderId(), INVENTORY_RESERVATION_REQUESTED_EVENT)
                || reservationRepository.findByOrderId(event.orderId()).isPresent()) {
            return;
        }

        InventoryReservation reservation = new InventoryReservation();
        reservation.setOrderId(event.orderId());
        reservation.setUserId(event.userId());
        reservation.setStatus(ReservationStatus.PENDING);

        event.items().forEach(item -> {
            InventoryReservationItem reservationItem = new InventoryReservationItem();
            reservationItem.setReservation(reservation);
            reservationItem.setVariantId(item.variantId());
            reservationItem.setProductName(item.productName());
            reservationItem.setQuantity(item.quantity());
            reservation.getItems().add(reservationItem);
        });

        String failureReason = null;
        try {
            adjustStock(event.items());
            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservation.setReason("Inventory reserved successfully");
        } catch (HttpClientErrorException ex) {
            failureReason = ex.getMessage();
            reservation.setStatus(ReservationStatus.REJECTED);
            reservation.setReason(failureReason);
        }

        reservationRepository.save(reservation);
        markEventProcessed(event.orderId(), INVENTORY_RESERVATION_REQUESTED_EVENT);
        kafkaTemplate.send(inventoryReservationCompletedTopic, event.orderId().toString(), new InventoryReservationCompletedEvent(
                event.orderId(), event.userId(), failureReason == null, failureReason, LocalDateTime.now()
        ));
    }

    private void adjustStock(List<OrderItemEvent> items) {
        List<StockAdjustmentRequest> adjustments = items.stream().map(item -> new StockAdjustmentRequest(item.variantId(), -item.quantity())).toList();
        restTemplate.exchange(productServiceUrl + "/internal/variants/stock-adjustments", HttpMethod.POST, new HttpEntity<>(adjustments), Void.class);
    }

    private void markEventProcessed(UUID aggregateId, String eventType) {
        ProcessedEventLog eventLog = new ProcessedEventLog();
        eventLog.setAggregateId(aggregateId);
        eventLog.setEventType(eventType);
        processedEventLogRepository.save(eventLog);
    }

    private InventoryReservationResponse toResponse(InventoryReservation reservation) {
        return new InventoryReservationResponse(
                reservation.getId(),
                reservation.getOrderId(),
                reservation.getUserId(),
                reservation.getStatus(),
                reservation.getReason(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt(),
                reservation.getItems().stream().map(item -> new InventoryReservationItemResponse(item.getVariantId(), item.getProductName(), item.getQuantity())).toList()
        );
    }
}
