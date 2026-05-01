package com.karn01.inventoryservice.controller;

import com.karn01.inventoryservice.dto.ApiResponse;
import com.karn01.inventoryservice.dto.InventoryReservationResponse;
import com.karn01.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping("/reservations/order/{orderId}")
    public ApiResponse<InventoryReservationResponse> getReservation(@PathVariable UUID orderId) {
        return new ApiResponse<>(true, "Inventory reservation fetched successfully", inventoryService.getReservation(orderId));
    }
}
