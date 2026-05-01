package com.karn01.orderservice.dto;

public record ApiResponse<T>(boolean success, String message, T data) {
}
