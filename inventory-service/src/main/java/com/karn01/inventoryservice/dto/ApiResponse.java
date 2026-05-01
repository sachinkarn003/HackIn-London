package com.karn01.inventoryservice.dto;

public record ApiResponse<T>(boolean success, String message, T data) {
}
