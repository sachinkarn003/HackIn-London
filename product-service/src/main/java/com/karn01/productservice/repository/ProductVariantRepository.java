package com.karn01.productservice.repository;

import com.karn01.productservice.entity.Product;
import com.karn01.productservice.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    @Query("""
    SELECT DISTINCT v.product
    FROM ProductVariant v
    WHERE (:size IS NULL OR v.size = :size)
      AND (:color IS NULL OR v.color = :color)
    """)
    List<Product> findProductByVariantFilter(String size, String color);
    List<ProductVariant> findByProduct(Product product);

    @Modifying
    @Query("""
        UPDATE ProductVariant v
        SET v.stock = v.stock + :quantityDelta
        WHERE v.id = :variantId
          AND v.stock + :quantityDelta >= 0
    """)
    int adjustStockIfAvailable(@Param("variantId") UUID variantId, @Param("quantityDelta") int quantityDelta);

    void deleteByProduct(Product product);
}
