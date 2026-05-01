package com.karn01.productservice.service;

import com.karn01.productservice.dto.CreateProductDto;
import com.karn01.productservice.dto.CreateVariantDto;
import com.karn01.productservice.dto.ProductResponseDto;
import com.karn01.productservice.dto.StockAdjustmentRequest;
import com.karn01.productservice.dto.VariantDetailDto;
import com.karn01.productservice.entity.Category;
import com.karn01.productservice.entity.Product;
import com.karn01.productservice.entity.ProductImage;
import com.karn01.productservice.entity.ProductVariant;
import com.karn01.productservice.exception.BadRequestException;
import com.karn01.productservice.exception.ResourceNotFoundException;
import com.karn01.productservice.repository.CategoryRepository;
import com.karn01.productservice.repository.ProductImageRepository;
import com.karn01.productservice.repository.ProductRepository;
import com.karn01.productservice.repository.ProductVariantRepository;
import com.karn01.productservice.search.ProductDocument;
import com.karn01.productservice.search.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final ProductSearchRepository productSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Transactional
    public void createProduct(CreateProductDto dto) {
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Product product = new Product();
        product.setCategory(category);
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setBrand(dto.getBrand());
        productRepository.save(product);

        dto.getImageUrls().forEach(url -> {
            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setImageUrl(url);
            imageRepository.save(image);
        });

        for (CreateVariantDto variantDto : dto.getVariants()) {
            ProductVariant variant = new ProductVariant();
            variant.setProduct(product);
            variant.setSize(variantDto.getSize());
            variant.setPrice(variantDto.getPrice());
            variant.setStock(variantDto.getStock());
            variant.setSku(variantDto.getSku());
            variant.setColor(variantDto.getColor());
            variantRepository.save(variant);
        }

        indexProduct(product, dto);
    }

    @Transactional
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        imageRepository.deleteByProduct(product);
        variantRepository.deleteByProduct(product);
        productRepository.delete(product);
        productSearchRepository.deleteById(id.toString());
    }

    @Transactional
    public void updateProduct(UUID id, CreateProductDto dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        product.setCategory(category);
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setBrand(dto.getBrand());
        productRepository.save(product);

        imageRepository.deleteByProduct(product);
        variantRepository.deleteByProduct(product);
        imageRepository.flush();
        variantRepository.flush();

        dto.getImageUrls().forEach(url -> {
            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setImageUrl(url);
            imageRepository.save(image);
        });

        for (CreateVariantDto variantDto : dto.getVariants()) {
            ProductVariant variant = new ProductVariant();
            variant.setProduct(product);
            variant.setSize(variantDto.getSize());
            variant.setPrice(variantDto.getPrice());
            variant.setStock(variantDto.getStock());
            variant.setSku(variantDto.getSku());
            variant.setColor(variantDto.getColor());
            variantRepository.save(variant);
        }

        indexProduct(product, dto);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProducts(Pageable pageable) {
        Page<Product> products = productRepository.findAll(pageable);
        return products.stream().map(this::toProductResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return toProductResponse(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> filterProducts(String size, String color, String categoryName) {
        List<Product> products = variantRepository.findProductByVariantFilter(size, color);
        if (categoryName != null && !categoryName.isBlank()) {
            products = products.stream()
                    .filter(product -> product.getCategory().getName().equalsIgnoreCase(categoryName))
                    .toList();
        }
        return products.stream().map(this::toProductResponse).toList();
    }

    public List<ProductDocument> search(String queryText, int page, int size) {
        Query query = NativeQuery.builder()
                .withQuery(q -> q.multiMatch(m -> m.query(queryText).fields("name", "description", "brand", "category")))
                .withPageable(PageRequest.of(page, size))
                .build();
        SearchHits<ProductDocument> hits = elasticsearchOperations.search(query, ProductDocument.class);
        return hits.stream().map(hit -> hit.getContent()).toList();
    }

    @Transactional(readOnly = true)
    public VariantDetailDto getVariant(UUID variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        return VariantDetailDto.builder()
                .id(variant.getId())
                .productName(variant.getProduct().getName())
                .price(variant.getPrice())
                .stock(variant.getStock())
                .build();
    }

    @Transactional(readOnly = true)
    public List<VariantDetailDto> getVariants(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("Variant ids are required");
        }
        return variantRepository.findAllById(ids).stream()
                .map(variant -> VariantDetailDto.builder()
                        .id(variant.getId())
                        .productName(variant.getProduct().getName())
                        .price(variant.getPrice())
                        .stock(variant.getStock())
                        .build())
                .toList();
    }

    @Transactional
    public void adjustStocks(List<StockAdjustmentRequest> adjustments) {
        if (adjustments == null || adjustments.isEmpty()) {
            throw new BadRequestException("Stock adjustments are required");
        }
        for (StockAdjustmentRequest adjustment : adjustments) {
            if (!variantRepository.existsById(adjustment.variantId())) {
                throw new ResourceNotFoundException("Variant not found: " + adjustment.variantId());
            }

            int updatedRows = variantRepository.adjustStockIfAvailable(adjustment.variantId(), adjustment.quantityDelta());
            if (updatedRows == 0) {
                throw new BadRequestException("Insufficient stock for variant " + adjustment.variantId());
            }
        }
    }

    private void indexProduct(Product product, CreateProductDto dto) {
        List<String> sizes = dto.getVariants().stream().map(CreateVariantDto::getSize).distinct().toList();
        List<String> colors = dto.getVariants().stream().map(CreateVariantDto::getColor).distinct().toList();
        BigDecimal minPrice = dto.getVariants().stream().map(CreateVariantDto::getPrice).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        ProductDocument document = ProductDocument.builder()
                .id(product.getId().toString())
                .name(product.getName())
                .description(product.getDescription())
                .brand(product.getBrand())
                .category(product.getCategory().getName())
                .sizes(sizes)
                .colors(colors)
                .minPrice(minPrice)
                .build();

        productSearchRepository.save(document);
    }

    private ProductResponseDto toProductResponse(Product product) {
        List<String> images = imageRepository.findByProduct(product).stream().map(ProductImage::getImageUrl).toList();
        List<ProductResponseDto.VariantDto> variants = variantRepository.findByProduct(product).stream()
                .map(variant -> ProductResponseDto.VariantDto.builder()
                        .id(variant.getId())
                        .size(variant.getSize())
                        .color(variant.getColor())
                        .price(variant.getPrice())
                        .stock(variant.getStock())
                        .build())
                .toList();

        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .brand(product.getBrand())
                .category(product.getCategory().getName())
                .images(images)
                .variants(variants)
                .build();
    }
}
