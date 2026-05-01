package com.karn01.productservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Setter
@Getter
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    private Product product;

    private String size;
    private String color;

    private BigDecimal price;

    private int stock;

    @Column(unique = true)
    private String sku;
}
