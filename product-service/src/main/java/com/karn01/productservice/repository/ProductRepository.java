package com.karn01.productservice.repository;

import com.karn01.productservice.entity.Product;
import com.karn01.productservice.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    long countByCategory(Category category);
}
