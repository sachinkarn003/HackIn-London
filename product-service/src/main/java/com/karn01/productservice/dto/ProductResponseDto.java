package com.karn01.productservice.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProductResponseDto {
    private UUID id;
    private String name;
    private String description;
    private String brand;
    private String category;

    private List<String> images;
    private List<VariantDto> variants;

    @Data
    @Builder
    public static class VariantDto {
        private UUID id;
        private String size;
        private String color;
        private BigDecimal price;
        private int stock;
    }
}
