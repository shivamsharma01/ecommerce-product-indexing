package com.mcart.product_indexer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductEventPayload {

    private String productId;
    private String eventType;
    private long version;
    private Instant updatedAt;

    private String name;
    private String description;
    private BigDecimal price;
    private String sku;
    private Integer stockQuantity;
    private List<String> categories;

    private String brand;
    private List<GalleryImagePayload> gallery;
    private Double rating;
    private Boolean inStock;
    private Map<String, Object> attributes;
}
