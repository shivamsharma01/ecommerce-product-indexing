package com.mcart.product_indexer.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Document(collectionName = "products")
@Getter
@Setter
@NoArgsConstructor
public class ProductFirestoreDocument {

    @DocumentId
    private String productId;

    private String name;
    private String description;
    private Double price;
    private String sku;
    private Integer stockQuantity;
    private List<String> categories;
    private String brand;
    private List<ProductGalleryImage> gallery;
    private Double rating;
    private Map<String, Object> attributes;
    private Boolean inStock;
    private long version;
    private Date createdAt;
    private Date updatedAt;
}
