package com.karn01.cartservice.service;

import com.karn01.cartservice.dto.AddToCartDto;
import com.karn01.cartservice.dto.CartItemResponse;
import com.karn01.cartservice.dto.VariantDetailDto;
import com.karn01.cartservice.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {
    private final RestTemplate restTemplate;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.services.product-service-url}")
    private String productServiceUrl;

    public void addToCart(String userId, AddToCartDto dto) {
        VariantDetailDto variant = getVariantDetails(dto.getVariantId());
        if (variant == null) {
            throw new BadRequestException("Variant not found");
        }

        String key = "cart:" + userId;
        Object existingQuantityValue = redisTemplate.opsForHash().get(key, dto.getVariantId().toString());
        int existingQuantity = existingQuantityValue == null ? 0 : Integer.parseInt(existingQuantityValue.toString());
        int requestedQuantity = existingQuantity + dto.getQuantity();

        if (requestedQuantity > variant.getStock()) {
            throw new BadRequestException("Not enough stock available");
        }

        redisTemplate.opsForHash().increment(key, dto.getVariantId().toString(), dto.getQuantity());
    }

    public void setQuantity(String userId, AddToCartDto dto) {
        VariantDetailDto variant = getVariantDetails(dto.getVariantId());
        if (variant == null) {
            throw new BadRequestException("Variant not found");
        }
        if (dto.getQuantity() < 1) {
            removeItem(userId, dto.getVariantId().toString());
            return;
        }
        if (dto.getQuantity() > variant.getStock()) {
            throw new BadRequestException("Not enough stock available");
        }

        redisTemplate.opsForHash().put("cart:" + userId, dto.getVariantId().toString(), String.valueOf(dto.getQuantity()));
    }

    public List<CartItemResponse> getCart(String userId) {
        String key = "cart:" + userId;
        Map<Object, Object> cart = redisTemplate.opsForHash().entries(key);
        List<CartItemResponse> response = new ArrayList<>();

        for (Map.Entry<Object, Object> entry : cart.entrySet()) {
            UUID variantId = UUID.fromString(entry.getKey().toString());
            int quantity = Integer.parseInt(entry.getValue().toString());
            VariantDetailDto variant = getVariantDetails(variantId);
            if (variant == null) {
                redisTemplate.opsForHash().delete(key, entry.getKey());
                continue;
            }
            response.add(new CartItemResponse(
                    variantId,
                    quantity,
                    variant.getPrice(),
                    variant.getProductName(),
                    variant.getPrice().multiply(BigDecimal.valueOf(quantity))
            ));
        }
        return response;
    }

    public void removeItem(String userId, String variantId) {
        redisTemplate.opsForHash().delete("cart:" + userId, variantId);
    }

    public void clearCart(String userId) {
        redisTemplate.delete("cart:" + userId);
    }

    private VariantDetailDto getVariantDetails(UUID variantId) {
        try {
            return restTemplate.getForObject(productServiceUrl + "/internal/variants/" + variantId, VariantDetailDto.class);
        } catch (HttpClientErrorException.NotFound ex) {
            return null;
        } catch (Exception ex) {
            throw new BadRequestException("Unable to fetch variant details right now");
        }
    }
}
