package com.karn01.productservice.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class VariantDetailDto {
    private UUID id;
    private String productName;
    private BigDecimal price;
    private int stock;
}
