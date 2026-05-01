package com.karn01.productservice.dto;

import lombok.Data;

@Data
public class CreateCategoryDto {
    private String name;
    private String description;
    private String imageUrl;
}
