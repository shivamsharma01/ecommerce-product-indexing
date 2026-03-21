package com.mcart.product_indexer.service;

import com.mcart.product_indexer.model.Product;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class ProductCoreFieldMapping {

    private ProductCoreFieldMapping() {
    }

    static void apply(
            Product product,
            String name,
            String description,
            Double price,
            List<String> categories,
            String legacyCategory,
            String brand,
            String imageUrl,
            Double rating,
            Map<String, Object> attributes,
            Boolean inStock,
            Integer stockQuantity,
            Long version,
            Instant updatedAt) {
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        applyCategories(product, categories, legacyCategory);
        product.setBrand(brand);
        product.setImageUrl(imageUrl);
        product.setRating(rating);
        product.setAttributes(attributes);
        applyStock(product, inStock, stockQuantity);
        product.setVersion(version);
        product.setUpdatedAt(updatedAt);
    }

    private static void applyCategories(Product product, List<String> categories, String legacyCategory) {
        if (categories != null && !categories.isEmpty()) {
            product.setCategories(categories);
        } else if (legacyCategory != null) {
            product.setCategories(Collections.singletonList(legacyCategory));
        } else {
            product.setCategories(Collections.emptyList());
        }
    }

    private static void applyStock(Product product, Boolean inStock, Integer stockQuantity) {
        product.setInStock(Objects.requireNonNullElseGet(inStock, () -> stockQuantity != null && stockQuantity > 0));
    }
}
