package com.karn01.productservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;

    @Column(length = 2000)
    private String description;

    private String brand;

    private boolean active = true;

    @ManyToOne
    private Category category;

    private LocalDateTime createdAt = LocalDateTime.now();
}
