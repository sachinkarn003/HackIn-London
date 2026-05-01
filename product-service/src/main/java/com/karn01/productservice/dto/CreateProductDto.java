package com.karn01.productservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateProductDto {
    @NotBlank(message = "Product name is required")
    private String name;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Brand is required")
    private String brand;

    @NotNull(message = "Category id is required")
    private UUID categoryId;

    @NotEmpty(message = "At least one image URL is required")
    private List<String> imageUrls;

    @Valid
    @NotEmpty(message = "At least one variant is required")
    private List<CreateVariantDto> variants;
}
