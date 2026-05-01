package com.karn01.orderservice.controller;

import com.karn01.orderservice.dto.ApiResponse;
import com.karn01.orderservice.dto.CheckoutRequest;
import com.karn01.orderservice.dto.OrderResponse;
import com.karn01.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/checkout")
    public ApiResponse<OrderResponse> checkout(@RequestHeader("X-User-Id") String userId,
                                               @Valid @RequestBody CheckoutRequest request) {
        return new ApiResponse<>(true, "Order created successfully", orderService.checkout(userId, request));
    }

    @GetMapping
    public ApiResponse<List<OrderResponse>> getUserOrders(@RequestHeader("X-User-Id") String userId) {
        return new ApiResponse<>(true, "Orders fetched successfully", orderService.getOrdersForUser(userId));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getOrder(@RequestHeader("X-User-Id") String userId,
                                               @PathVariable UUID orderId) {
        return new ApiResponse<>(true, "Order fetched successfully", orderService.getOrderForUser(userId, orderId));
    }
}
