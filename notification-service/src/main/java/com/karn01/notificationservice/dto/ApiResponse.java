package com.karn01.notificationservice.dto;

public record ApiResponse<T>(boolean success, String message, T data) {
}
