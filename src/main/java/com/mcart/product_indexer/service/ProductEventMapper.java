package com.mcart.product_indexer.service;

import com.mcart.product_indexer.dto.ProductEventPayload;
import com.mcart.product_indexer.model.Product;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

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

        List<String> categories = payload.getCategories();
        if (categories != null && !categories.isEmpty()) {
            product.setCategories(categories);
        } else if (payload.getCategory() != null) {
            product.setCategories(Collections.singletonList(payload.getCategory()));
        } else {
            product.setCategories(Collections.emptyList());
        }

        product.setBrand(payload.getBrand());
        product.setImageUrl(payload.getImageUrl());
        product.setRating(payload.getRating());
        product.setAttributes(payload.getAttributes());

        if (payload.getInStock() != null) {
            product.setInStock(payload.getInStock());
        } else {
            product.setInStock(payload.getStockQuantity() != null && payload.getStockQuantity() > 0);
        }

        product.setVersion(payload.getVersion());
        product.setUpdatedAt(payload.getUpdatedAt());

        return product;
    }
}
