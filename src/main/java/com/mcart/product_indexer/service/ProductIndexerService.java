package com.mcart.product_indexer.service;

import com.mcart.product_indexer.dto.CloudEventEnvelope;
import com.mcart.product_indexer.dto.DocumentEventData;
import com.mcart.product_indexer.dto.FirestoreDocument;
import com.mcart.product_indexer.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

/**
 * Processes Firestore document events and indexes/updates/deletes products in Elasticsearch.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductIndexerService {

    private static final String WRITTEN_EVENT = "google.cloud.firestore.document.v1.written";
    private static final String DELETED_EVENT = "google.cloud.firestore.document.v1.deleted";
    private static final String PRODUCTS_INDEX = "products";

    private final FirestoreToProductMapper firestoreToProductMapper;
    private final ElasticsearchOperations elasticsearchOperations;

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
     * Processes a CloudEvent envelope (Firestore document event).
     *
     * @param envelope the CloudEvent from Pub/Sub
     * @return true if processed successfully
     */
    public boolean processEvent(CloudEventEnvelope envelope) {
        if (envelope == null || envelope.getData() == null) {
            log.warn("Invalid event: missing envelope or data");
            return false;
        }

        String type = envelope.getType();
        String subject = envelope.getSubject();
        if (subject == null || !subject.contains("/products/")) {
            log.debug("Skipping non-product event, subject: {}", subject);
            return true; // ack to avoid reprocessing
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
            return handleWrite(documentId, doc);
        }

        log.debug("Ignoring event type: {}", type);
        return true;
    }

    private boolean handleWrite(String documentId, FirestoreDocument document) {
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
