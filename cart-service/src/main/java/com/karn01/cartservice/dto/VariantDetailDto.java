package com.karn01.cartservice.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class VariantDetailDto {
    private UUID id;
    private String productName;
    private BigDecimal price;
    private int stock;
}
