package com.karn01.productservice.search;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.elasticsearch.annotations.Document;

import java.math.BigDecimal;
import java.util.List;

@Document(indexName = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDocument {
    @Id
    private String id;

    private String name;
    private String description;
    private String brand;
    private String category;

    private List<String> sizes;
    private List<String> colors;
    private BigDecimal minPrice;
}
