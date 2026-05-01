package com.karn01.orderservice.repository;

import com.karn01.orderservice.entity.ProcessedEventLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventLogRepository extends JpaRepository<ProcessedEventLog, UUID> {
    boolean existsByAggregateIdAndEventType(UUID aggregateId, String eventType);
}
