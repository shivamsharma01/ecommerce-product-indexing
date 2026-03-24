package com.mcart.product_indexer.service;

import com.mcart.product_indexer.model.Product;
import com.mcart.product_indexer.model.ProductFirestoreDocument;
import org.springframework.stereotype.Component;

@Component
public class FirestoreProductDocumentMapper {

    public Product toProduct(ProductFirestoreDocument doc) {
        if (doc == null) {
            return null;
        }

        Product product = new Product();
        product.setId(doc.getProductId());
        ProductCoreFieldMapping.apply(
                product,
                doc.getName(),
                doc.getDescription(),
                doc.getPrice(),
                doc.getCategories(),
                doc.getBrand(),
                doc.getImageUrls(),
                doc.getRating(),
                doc.getAttributes(),
                doc.getInStock(),
                doc.getStockQuantity(),
                doc.getVersion(),
                doc.getUpdatedAt() != null ? doc.getUpdatedAt().toInstant() : null);
        return product;
    }
}
