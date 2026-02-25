package com.mcart.product_indexer.service;

import com.mcart.product_indexer.config.ReindexGate;
import com.mcart.product_indexer.repository.ProductFirestoreRepository;
import com.mcart.product_indexer.model.Product;
import com.mcart.product_indexer.model.ProductFirestoreDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReindexService {

    private static final String PRODUCTS_INDEX = "products";

    private final ProductFirestoreRepository productFirestoreRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ReindexGate reindexGate;

    public long reindex() {
        log.info("Starting full reindex (delete index, recreate, index all)");
        reindexGate.setReindexInProgress(true);
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(Product.class);

            if (indexOps.exists()) {
                indexOps.delete();
                log.info("Deleted products index");
            }

            // 2. Recreate index with mapping
            indexOps.createWithMapping();
            log.info("Created products index with mapping");

            // 3. Index all products from Firestore (same FirestoreTemplate as product service)
            Long count = productFirestoreRepository.findAll()
                    .doOnNext(doc -> indexProduct(doc).blockLast())
                    .count()
                    .block();
            log.info("Reindex complete: indexed {} products", count);
            return count != null ? count : 0;
        } finally {
            reindexGate.setReindexInProgress(false);
        }
    }

    private Flux<Void> indexProduct(ProductFirestoreDocument doc) {
        try {
            Product product = toProduct(doc);
            elasticsearchOperations.save(product, IndexCoordinates.of(PRODUCTS_INDEX));
            log.debug("Indexed product: id={}", doc.getProductId());
            return Flux.empty();
        } catch (Exception e) {
            log.error("Failed to index product id={}", doc.getProductId(), e);
            return Flux.empty();
        }
    }

    private Product toProduct(ProductFirestoreDocument doc) {
        Product product = new Product();
        product.setId(doc.getProductId());
        product.setName(doc.getName());
        product.setDescription(doc.getDescription());
        product.setPrice(doc.getPrice());
        product.setCategories(doc.getCategory() != null ? Collections.singletonList(doc.getCategory()) : Collections.emptyList());
        product.setInStock(doc.getStockQuantity() != null && doc.getStockQuantity() > 0);
        product.setVersion(doc.getVersion());
        product.setUpdatedAt(doc.getUpdatedAt() != null ? doc.getUpdatedAt().toInstant() : null);
        return product;
    }
}
