package com.karn01.orderservice.entity;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAYMENT_FAILED,
    PAYMENT_COMPLETED,
    CONFIRMED,
    REJECTED,
    REFUNDED
}
