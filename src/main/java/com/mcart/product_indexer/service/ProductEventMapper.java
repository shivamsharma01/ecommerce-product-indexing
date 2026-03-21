package com.mcart.product_indexer.service;

import com.mcart.product_indexer.dto.ProductEventPayload;
import com.mcart.product_indexer.model.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductEventMapper {

    public Product toProduct(ProductEventPayload payload) {
        if (payload == null || payload.getProductId() == null) {
            return null;
        }

        Product product = new Product();
        product.setId(payload.getProductId());
        ProductCoreFieldMapping.apply(
                product,
                payload.getName(),
                payload.getDescription(),
                payload.getPrice() != null ? payload.getPrice().doubleValue() : null,
                payload.getCategories(),
                payload.getCategory(),
                payload.getBrand(),
                payload.getImageUrl(),
                payload.getRating(),
                payload.getAttributes(),
                payload.getInStock(),
                payload.getStockQuantity(),
                payload.getVersion(),
                payload.getUpdatedAt());
        return product;
    }
}
