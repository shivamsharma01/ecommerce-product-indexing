package com.mcart.product_indexer.service;

import com.mcart.product_indexer.dto.FirestoreDocument;
import com.mcart.product_indexer.dto.FirestoreValue;
import com.mcart.product_indexer.model.Product;
import com.mcart.product_indexer.model.ProductGalleryImage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FirestoreToProductMapper {

    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_PRICE = "price";
    private static final String FIELD_CATEGORIES = "categories";
    private static final String FIELD_STOCK_QUANTITY = "stockQuantity";
    private static final String FIELD_IN_STOCK = "inStock";
    private static final String FIELD_BRAND = "brand";
    private static final String FIELD_GALLERY = "gallery";
    private static final String FIELD_RATING = "rating";
    private static final String FIELD_ATTRIBUTES = "attributes";

    public String resolveDocumentId(String pathDocumentId, FirestoreDocument document) {
        if (document != null && document.getFields() != null) {
            FirestoreValue idVal = document.getFields().get(FIELD_ID);
            if (idVal != null && !idVal.isNull()) {
                String s = idVal.getString();
                if (s != null && !s.isEmpty()) return s;
                Long l = idVal.getLong();
                if (l != null) return String.valueOf(l);
            }
        }
        return pathDocumentId;
    }

    public Product map(String documentId, FirestoreDocument document) {
        if (document == null || document.getFields() == null) {
            return null;
        }

        Map<String, FirestoreValue> fields = document.getFields();
        Product product = new Product();
        product.setId(documentId);

        product.setName(getString(fields, FIELD_NAME));
        product.setDescription(getString(fields, FIELD_DESCRIPTION));
        product.setPrice(getDouble(fields, FIELD_PRICE));

        product.setCategories(getStringList(fields, FIELD_CATEGORIES));

        product.setBrand(getString(fields, FIELD_BRAND));
        product.setGallery(getGalleryList(fields, FIELD_GALLERY));
        product.setRating(getDouble(fields, FIELD_RATING));

        FirestoreValue inStockVal = fields.get(FIELD_IN_STOCK);
        product.setInStock(inStockVal != null && Boolean.TRUE.equals(inStockVal.getBoolean()));

        product.setAttributes(getMapValue(fields, FIELD_ATTRIBUTES));

        Long version = getLong(fields, "version");
        if (version != null) product.setVersion(version);

        return product;
    }

    private String getString(Map<String, FirestoreValue> fields, String key) {
        FirestoreValue v = fields.get(key);
        return v != null ? v.getString() : null;
    }

    private Integer getInteger(Map<String, FirestoreValue> fields, String key) {
        FirestoreValue v = fields.get(key);
        return v != null ? v.getInteger() : null;
    }

    private Long getLong(Map<String, FirestoreValue> fields, String key) {
        FirestoreValue v = fields.get(key);
        return v != null ? v.getLong() : null;
    }

    private Double getDouble(Map<String, FirestoreValue> fields, String key) {
        FirestoreValue v = fields.get(key);
        return v != null ? v.getDouble() : null;
    }

    @SuppressWarnings("unchecked")
    private List<String> getStringList(Map<String, FirestoreValue> fields, String key) {
        FirestoreValue v = fields.get(key);
        if (v == null || v.getArrayValue() == null || v.getArrayValue().getValues() == null) {
            return null;
        }
        return v.getArrayValue().getValues().stream()
                .map(FirestoreValue::getString)
                .filter(s -> s != null)
                .collect(Collectors.toList());
    }

    private List<ProductGalleryImage> getGalleryList(Map<String, FirestoreValue> fields, String key) {
        FirestoreValue v = fields.get(key);
        if (v == null || v.getArrayValue() == null || v.getArrayValue().getValues() == null) {
            return List.of();
        }
        List<ProductGalleryImage> out = new ArrayList<>();
        for (FirestoreValue item : v.getArrayValue().getValues()) {
            if (item == null || item.getMapValue() == null || item.getMapValue().getFields() == null) {
                continue;
            }
            Map<String, FirestoreValue> map = item.getMapValue().getFields();
            ProductGalleryImage image = new ProductGalleryImage();
            image.setThumbnailUrl(getString(map, "thumbnailUrl"));
            image.setHdUrl(getString(map, "hdUrl"));
            image.setAlt(getString(map, "alt"));
            out.add(image);
        }
        return out;
    }

    private Map<String, Object> getMapValue(Map<String, FirestoreValue> fields, String key) {
        FirestoreValue v = fields.get(key);
        if (v == null || v.getMapValue() == null || v.getMapValue().getFields() == null) {
            return null;
        }
        return firestoreMapToPlainMap(v.getMapValue().getFields());
    }

    private Map<String, Object> firestoreMapToPlainMap(Map<String, FirestoreValue> fields) {
        if (fields == null) return null;
        return fields.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isNull())
                .collect(Collectors.toMap(Map.Entry::getKey, e -> toPlainValue(e.getValue())));
    }

    private Object toPlainValue(FirestoreValue v) {
        if (v == null || v.isNull()) return null;
        if (v.getStringValue() != null) return v.getStringValue();
        if (v.getIntegerValue() != null) return v.getLong();
        if (v.getDoubleValue() != null) return v.getDoubleValue();
        if (v.getBooleanValue() != null) return v.getBooleanValue();
        if (v.getArrayValue() != null && v.getArrayValue().getValues() != null) {
            return v.getArrayValue().getValues().stream()
                    .map(this::toPlainValue)
                    .collect(Collectors.toList());
        }
        if (v.getMapValue() != null) {
            return firestoreMapToPlainMap(v.getMapValue().getFields());
        }
        return null;
    }
}
