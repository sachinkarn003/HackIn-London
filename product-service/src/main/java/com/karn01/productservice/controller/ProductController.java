package com.karn01.productservice.controller;

import com.karn01.productservice.dto.CreateProductDto;
import com.karn01.productservice.dto.ProductResponseDto;
import com.karn01.productservice.dto.StockAdjustmentRequest;
import com.karn01.productservice.dto.VariantDetailDto;
import com.karn01.productservice.search.ProductDocument;
import com.karn01.productservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping
    public String createProduct(@Valid @RequestBody CreateProductDto dto) {
        productService.createProduct(dto);
        return "Product created successfully";
    }

    @GetMapping
    public List<ProductResponseDto> getProducts(Pageable pageable) {
        return productService.getAllProducts(pageable);
    }

    @GetMapping("/{id}")
    public ProductResponseDto getProduct(@PathVariable UUID id) {
        return productService.getProductById(id);
    }

    @DeleteMapping("/{id}")
    public String deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return "Product deleted successfully";
    }

    @PutMapping("/{id}")
    public String updateProduct(@PathVariable UUID id, @Valid @RequestBody CreateProductDto dto) {
        productService.updateProduct(id, dto);
        return "Product updated successfully";
    }

    @GetMapping("/filter")
    public List<ProductResponseDto> filterProducts(@RequestParam(required = false) String size,
                                                   @RequestParam(required = false) String color,
                                                   @RequestParam(required = false) String category) {
        return productService.filterProducts(size, color, category);
    }

    @GetMapping("/search")
    public List<ProductDocument> search(@RequestParam String q,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        return productService.search(q, page, size);
    }

    @GetMapping("/internal/variants/{id}")
    public VariantDetailDto getVariant(@PathVariable UUID id) {
        return productService.getVariant(id);
    }

    @PostMapping("/internal/variants/details")
    public List<VariantDetailDto> getVariants(@RequestBody List<UUID> ids) {
        return productService.getVariants(ids);
    }

    @PostMapping("/internal/variants/stock-adjustments")
    public void adjustStocks(@RequestBody List<StockAdjustmentRequest> adjustments) {
        productService.adjustStocks(adjustments);
    }
}
