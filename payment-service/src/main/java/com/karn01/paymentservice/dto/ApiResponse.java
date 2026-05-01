package com.karn01.paymentservice.dto;

public record ApiResponse<T>(boolean success, String message, T data) {
}
