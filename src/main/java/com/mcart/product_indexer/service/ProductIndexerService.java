package com.mcart.product_indexer.service;

import com.mcart.product_indexer.dto.ProductEventEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcart.product_indexer.config.ReindexGate;
import com.mcart.product_indexer.dto.ProductEventPayload;
import com.mcart.product_indexer.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductIndexerService {

    private static final String PRODUCTS_INDEX = "products";

    private final ProductEventMapper productEventMapper;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ObjectMapper objectMapper;
    private final ReindexGate reindexGate;

    @PostConstruct
    public void ensureIndexExists() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(Product.class);
        if (!indexOps.exists()) {
            indexOps.createWithMapping();
            log.info("Created Elasticsearch index: {}", PRODUCTS_INDEX);
        }
    }

    public boolean processMessage(String payloadJson) {
        if (reindexGate.isReindexInProgress()) {
            log.debug("Skipping message during reindex (will be redelivered)");
            return false;
        }
        if (payloadJson == null || payloadJson.isBlank()) {
            log.warn("Invalid message: empty payload");
            return false;
        }
        try {
            if (!payloadJson.contains("\"aggregateId\"") || !payloadJson.contains("\"payload\"")) {
                log.warn("Invalid message: expected ProductEventEnvelope format (aggregateId, payload)");
                return false;
            }
            ProductEventEnvelope envelope = objectMapper.readValue(payloadJson, ProductEventEnvelope.class);
            return processProductEvent(envelope);
        } catch (Exception e) {
            log.error("Failed to parse event payload", e);
            return false;
        }
    }

    public boolean processProductEvent(ProductEventEnvelope envelope) {
        if (envelope == null || envelope.getPayload() == null) {
            log.warn("Invalid product event: missing envelope or payload");
            return false;
        }

        ProductEventPayload payload = objectMapper.convertValue(envelope.getPayload(), ProductEventPayload.class);
        String productId = payload.getProductId();
        if (productId == null || productId.isEmpty()) {
            log.warn("Invalid product event: missing productId");
            return false;
        }

        String eventType = envelope.getEventType();
        if ("PRODUCT_DELETED".equals(eventType)) {
            return handleDelete(productId);
        }

        if ("PRODUCT_CREATED".equals(eventType) || "PRODUCT_UPDATED".equals(eventType)) {
            return handleProductEventWrite(payload);
        }

        log.debug("Ignoring event type: {}", eventType);
        return true;
    }

    private boolean handleProductEventWrite(ProductEventPayload payload) {
        Product product = productEventMapper.toProduct(payload);
        if (product == null) {
            return false;
        }

        String productId = product.getId();
        Optional<Product> existing = Optional.ofNullable(
                elasticsearchOperations.get(productId, Product.class, IndexCoordinates.of(PRODUCTS_INDEX)));

        if (existing.isPresent()) {
            Product existingProduct = existing.get();
            if (existingProduct.getVersion() != null && existingProduct.getVersion() > payload.getVersion()) {
                log.debug("Skipping older version: productId={} incoming={} existing={}",
                        productId, payload.getVersion(), existingProduct.getVersion());
                return true;
            }
            if (existingProduct.getUpdatedAt() != null && payload.getUpdatedAt() != null
                    && existingProduct.getUpdatedAt().isAfter(payload.getUpdatedAt())) {
                log.debug("Skipping older updatedAt: productId={}", productId);
                return true;
            }
        }

        try {
            elasticsearchOperations.save(product, IndexCoordinates.of(PRODUCTS_INDEX));
            log.info("Indexed product: id={} version={}", productId, payload.getVersion());
            return true;
        } catch (Exception e) {
            log.error("Failed to index product id={}", productId, e);
            throw e;
        }
    }

    private boolean handleDelete(String documentId) {
        try {
            elasticsearchOperations.delete(documentId, IndexCoordinates.of(PRODUCTS_INDEX));
            log.info("Deleted product from index: id={}", documentId);
            return true;
        } catch (NoSuchIndexException e) {
            log.debug("Index {} does not exist; delete is idempotent no-op for id={}", PRODUCTS_INDEX, documentId);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete product id={} from index", documentId, e);
            throw e;
        }
    }
}
