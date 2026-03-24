package com.mcart.product_indexer.service;

import com.mcart.product_indexer.model.Product;

import java.time.Instant;
import java.util.List;
import java.util.Map;

final class ProductCoreFieldMapping {

    private ProductCoreFieldMapping() {
    }

    static void apply(
            Product product,
            String name,
            String description,
            Double price,
            List<String> categories,
            String brand,
            List<String> imageUrls,
            Double rating,
            Map<String, Object> attributes,
            Boolean inStock,
            Integer stockQuantity,
            Long version,
            Instant updatedAt) {
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setCategories(categories);
        product.setBrand(brand);
        product.setImageUrls(imageUrls);
        product.setRating(rating);
        product.setAttributes(attributes);
        product.setInStock(inStock != null ? inStock : false);
        product.setVersion(version);
        product.setUpdatedAt(updatedAt);
    }
}
