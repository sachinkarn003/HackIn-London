package com.karn01.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private String userId;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false, length = 1000)
    private String message;
    @Column(nullable = false)
    private String type;
    private String referenceId;
    @Column(nullable = false)
    private boolean isRead;
    private LocalDateTime createdAt;
    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}
