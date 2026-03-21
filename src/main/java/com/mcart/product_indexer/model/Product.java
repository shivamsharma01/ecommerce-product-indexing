package com.mcart.product_indexer.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@Document(indexName = Product.INDEX_NAME)
public class Product {

    public static final String INDEX_NAME = "products";

    @Id
    private String id;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "standard"),
            otherFields = @InnerField(suffix = "sort", type = FieldType.Keyword))
    private String name;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private List<String> categories;

    @Field(type = FieldType.Keyword)
    private String brand;

    @Field(type = FieldType.Double)
    private Double price;

    @Field(type = FieldType.Keyword)
    private String imageUrl;

    @Field(type = FieldType.Double)
    private Double rating;

    @Field(type = FieldType.Boolean)
    private boolean inStock;

    @Field(type = FieldType.Object)
    private Map<String, Object> attributes;

    @Field(type = FieldType.Long)
    private Long version;

    @Field(type = FieldType.Date)
    private java.time.Instant updatedAt;
}
