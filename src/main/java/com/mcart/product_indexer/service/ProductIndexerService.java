package com.mcart.product_indexer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcart.product_indexer.config.ReindexGate;
import com.mcart.product_indexer.dto.ProductEventEnvelope;
import com.mcart.product_indexer.dto.ProductEventPayload;
import com.mcart.product_indexer.elasticsearch.ProductElasticsearchIndex;
import com.mcart.product_indexer.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductIndexerService {

    private final ProductEventMapper productEventMapper;
    private final ProductElasticsearchIndex productIndex;
    private final ObjectMapper objectMapper;
    private final ReindexGate reindexGate;

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
            JsonNode root = objectMapper.readTree(payloadJson);
            if (!root.isObject()) {
                log.warn("Invalid message: expected JSON object");
                return false;
            }
            if (!root.hasNonNull("aggregateId")) {
                log.warn("Invalid message: missing aggregateId");
                return false;
            }
            JsonNode aggregateId = root.get("aggregateId");
            if (aggregateId.isTextual() && aggregateId.asText().isBlank()) {
                log.warn("Invalid message: blank aggregateId");
                return false;
            }
            JsonNode payloadNode = root.get("payload");
            if (payloadNode == null || payloadNode.isNull() || !payloadNode.isObject()) {
                log.warn("Invalid message: missing or invalid payload object");
                return false;
            }
            ProductEventEnvelope envelope = objectMapper.treeToValue(root, ProductEventEnvelope.class);
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
            productIndex.deleteById(productId);
            return true;
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
        Optional<Product> existing = productIndex.findById(productId);

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
            productIndex.save(product);
            log.info("Indexed product: id={} version={}", productId, payload.getVersion());
            return true;
        } catch (Exception e) {
            log.error("Failed to index product id={}", productId, e);
            throw e;
        }
    }
}
