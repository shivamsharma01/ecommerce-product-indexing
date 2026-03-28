package com.mcart.product_indexer.service;

import com.mcart.product_indexer.dto.ProductEventPayload;
import com.mcart.product_indexer.model.Product;
import com.mcart.product_indexer.model.ProductGalleryImage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
                payload.getBrand(),
                toGallery(payload),
                payload.getRating(),
                payload.getAttributes(),
                payload.getInStock(),
                payload.getStockQuantity(),
                payload.getVersion(),
                payload.getUpdatedAt());
        return product;
    }

    private List<ProductGalleryImage> toGallery(ProductEventPayload payload) {
        List<ProductGalleryImage> out = new ArrayList<>();
        if (payload.getGallery() == null) {
            return out;
        }
        for (var item : payload.getGallery()) {
            ProductGalleryImage image = new ProductGalleryImage();
            image.setThumbnailUrl(item.getThumbnailUrl());
            image.setHdUrl(item.getHdUrl());
            image.setAlt(item.getAlt());
            out.add(image);
        }
        return out;
    }
}
