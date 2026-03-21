package com.mcart.product_indexer.service;

import com.mcart.product_indexer.config.ReindexGate;
import com.mcart.product_indexer.elasticsearch.ProductElasticsearchIndex;
import com.mcart.product_indexer.model.Product;
import com.mcart.product_indexer.model.ProductFirestoreDocument;
import com.mcart.product_indexer.repository.ProductFirestoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReindexService {

    private final ProductFirestoreRepository productFirestoreRepository;
    private final ProductElasticsearchIndex productIndex;
    private final FirestoreProductDocumentMapper firestoreProductDocumentMapper;
    private final ReindexGate reindexGate;

    public ReindexResult reindex() {
        log.info("Starting full reindex (delete index, recreate, index all)");
        reindexGate.setReindexInProgress(true);
        try {
            IndexOperations indexOps = productIndex.indexOperations();

            if (indexOps.exists()) {
                indexOps.delete();
                log.info("Deleted products index");
            }

            indexOps.createWithMapping();
            log.info("Created products index with mapping");

            long indexed = 0;
            long failed = 0;
            for (ProductFirestoreDocument doc : productFirestoreRepository.findAll().toIterable()) {
                if (doc == null) {
                    failed++;
                    log.warn("Skipping null Firestore document");
                    continue;
                }
                try {
                    Product product = firestoreProductDocumentMapper.toProduct(doc);
                    productIndex.save(product);
                    indexed++;
                    log.debug("Indexed product: id={}", doc.getProductId());
                } catch (Exception e) {
                    failed++;
                    log.error("Failed to index product id={}", doc.getProductId(), e);
                }
            }
            log.info("Reindex complete: indexed {} products, {} failed", indexed, failed);
            return new ReindexResult(indexed, failed);
        } finally {
            reindexGate.setReindexInProgress(false);
        }
    }
}
