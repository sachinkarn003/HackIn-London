package com.karn01.productservice.repository;

import com.karn01.productservice.entity.Product;
import com.karn01.productservice.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {
    List<ProductImage> findByProduct(Product product);
    void deleteByProduct(Product product);

}
