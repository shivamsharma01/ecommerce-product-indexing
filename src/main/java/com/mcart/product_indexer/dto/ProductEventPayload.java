package com.mcart.product_indexer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payload for product events from product service outbox.
 * Matches ProductEventPayload in product service.
 */
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
    private String category;
}
