package com.mcart.product_indexer.service;

import com.mcart.product_indexer.dto.CloudEventEnvelope;
import com.mcart.product_indexer.dto.DocumentEventData;
import com.mcart.product_indexer.dto.FirestoreDocument;
import com.mcart.product_indexer.dto.ProductEventEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcart.product_indexer.dto.ProductEventPayload;
import com.mcart.product_indexer.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Processes product events and indexes/updates/deletes products in Elasticsearch.
 * Supports both: (1) ProductEventEnvelope from product service outbox, (2) CloudEventEnvelope from Eventarc.
 * Uses productId as ES _id (idempotent). Rejects older versions during reindex.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductIndexerService {

    private static final String WRITTEN_EVENT = "google.cloud.firestore.document.v1.written";
    private static final String DELETED_EVENT = "google.cloud.firestore.document.v1.deleted";
    private static final String PRODUCTS_INDEX = "products";

    private final FirestoreToProductMapper firestoreToProductMapper;
    private final ProductEventMapper productEventMapper;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ObjectMapper objectMapper;

    /**
     * Extracts document ID from subject path.
     * Subject format: documents/projects/{project}/databases/{db}/documents/products/{id}
     */
    public static String extractDocumentId(String subject) {
        if (subject == null) return null;
        int lastSlash = subject.lastIndexOf('/');
        return lastSlash >= 0 ? subject.substring(lastSlash + 1) : subject;
    }

    /**
     * Processes a Pub/Sub message. Dispatches to ProductEventEnvelope or CloudEventEnvelope handler.
     */
    public boolean processMessage(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            log.warn("Invalid message: empty payload");
            return false;
        }
        try {
            // Try product service outbox format first (has eventType, aggregateId, payload at top level)
            if (payloadJson.contains("\"aggregateId\"") && payloadJson.contains("\"payload\"")) {
                ProductEventEnvelope envelope = objectMapper.readValue(payloadJson, ProductEventEnvelope.class);
                return processProductEvent(envelope);
            }
            // Fall back to CloudEvent (Eventarc) format
            CloudEventEnvelope envelope = objectMapper.readValue(payloadJson, CloudEventEnvelope.class);
            return processCloudEvent(envelope);
        } catch (Exception e) {
            log.error("Failed to parse event payload", e);
            return false;
        }
    }

    /**
     * Processes ProductEventEnvelope from product service outbox.
     */
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

    /**
     * Idempotent write: use productId as _id. Skip if ES doc has newer version/updatedAt.
     */
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

    /**
     * Processes a CloudEvent envelope (Firestore document event from Eventarc).
     */
    public boolean processCloudEvent(CloudEventEnvelope envelope) {
        if (envelope == null || envelope.getData() == null) {
            log.warn("Invalid event: missing envelope or data");
            return false;
        }

        String type = envelope.getType();
        String subject = envelope.getSubject();
        if (subject == null || !subject.contains("/products/")) {
            log.debug("Skipping non-product event, subject: {}", subject);
            return true;
        }

        DocumentEventData data = envelope.getData();
        String documentId = extractDocumentId(subject);

        if (documentId == null || documentId.isEmpty()) {
            log.warn("Could not extract document ID from subject: {}", subject);
            return false;
        }

        if (DELETED_EVENT.equals(type)) {
            return handleDelete(documentId);
        }

        if (WRITTEN_EVENT.equals(type)) {
            FirestoreDocument doc = data.getValue();
            return handleFirestoreWrite(documentId, doc);
        }

        log.debug("Ignoring event type: {}", type);
        return true;
    }

    /** @deprecated Use processCloudEvent for CloudEvent format. */
    @Deprecated
    public boolean processEvent(CloudEventEnvelope envelope) {
        return processCloudEvent(envelope);
    }

    private boolean handleFirestoreWrite(String documentId, FirestoreDocument document) {
        String esId = firestoreToProductMapper.resolveDocumentId(documentId, document);
        Product product = firestoreToProductMapper.map(esId, document);
        if (product == null) {
            log.warn("Could not map document {} to product", documentId);
            return false;
        }

        try {
            elasticsearchOperations.save(product, IndexCoordinates.of(PRODUCTS_INDEX));
            log.info("Indexed product: id={}", documentId);
            return true;
        } catch (Exception e) {
            log.error("Failed to index product id={}", documentId, e);
            throw e;
        }
    }

    private boolean handleDelete(String documentId) {
        try {
            elasticsearchOperations.delete(documentId, IndexCoordinates.of(PRODUCTS_INDEX));
            log.info("Deleted product from index: id={}", documentId);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete product id={} from index", documentId, e);
            throw e;
        }
    }
}
