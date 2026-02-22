package com.mcart.product_indexer.service;

import com.mcart.product_indexer.dto.ProductEventPayload;
import com.mcart.product_indexer.model.Product;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Maps ProductEventPayload (from product service outbox) to Elasticsearch Product.
 */
@Component
public class ProductEventMapper {

    public Product toProduct(ProductEventPayload payload) {
        if (payload == null || payload.getProductId() == null) {
            return null;
        }

        Product product = new Product();
        product.setId(payload.getProductId());
        product.setName(payload.getName());
        product.setDescription(payload.getDescription());
        product.setPrice(payload.getPrice() != null ? payload.getPrice().doubleValue() : null);
        product.setCategories(payload.getCategory() != null ? Collections.singletonList(payload.getCategory()) : Collections.emptyList());
        product.setInStock(payload.getStockQuantity() != null && payload.getStockQuantity() > 0);
        product.setVersion(payload.getVersion());
        product.setUpdatedAt(payload.getUpdatedAt());

        return product;
    }
}
