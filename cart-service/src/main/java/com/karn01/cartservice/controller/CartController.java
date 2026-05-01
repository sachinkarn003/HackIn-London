package com.karn01.cartservice.controller;

import com.karn01.cartservice.dto.AddToCartDto;
import com.karn01.cartservice.dto.CartItemResponse;
import com.karn01.cartservice.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @PostMapping("/add")
    public String add(@RequestHeader("X-User-Id") String userId, @Valid @RequestBody AddToCartDto dto) {
        cartService.addToCart(userId, dto);
        return "Added to cart";
    }

    @PutMapping("/quantity")
    public String setQuantity(@RequestHeader("X-User-Id") String userId, @Valid @RequestBody AddToCartDto dto) {
        cartService.setQuantity(userId, dto);
        return "Quantity updated";
    }

    @GetMapping
    public List<CartItemResponse> get(@RequestHeader("X-User-Id") String userId) {
        return cartService.getCart(userId);
    }

    @DeleteMapping("/{variantId}")
    public String remove(@RequestHeader("X-User-Id") String userId, @PathVariable String variantId) {
        cartService.removeItem(userId, variantId);
        return "Removed";
    }

    @DeleteMapping("/clear-cart")
    public String clear(@RequestHeader("X-User-Id") String userId) {
        cartService.clearCart(userId);
        return "Cart cleared";
    }

    @GetMapping("/internal/cart/{userId}")
    public List<CartItemResponse> getInternalCart(@PathVariable String userId) {
        return cartService.getCart(userId);
    }

    @DeleteMapping("/internal/cart/{userId}")
    public void clearInternalCart(@PathVariable String userId) {
        cartService.clearCart(userId);
    }
}
