package com.mcart.product_indexer.elasticsearch;

import com.mcart.product_indexer.model.Product;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductElasticsearchIndex {

    private final ElasticsearchOperations elasticsearchOperations;

    private static IndexCoordinates coordinates() {
        return IndexCoordinates.of(Product.INDEX_NAME);
    }

    @PostConstruct
    void ensureIndexExists() {
        IndexOperations indexOps = indexOperations();
        if (!indexOps.exists()) {
            indexOps.createWithMapping();
            log.info("Created Elasticsearch index: {}", Product.INDEX_NAME);
        }
    }

    public IndexOperations indexOperations() {
        return elasticsearchOperations.indexOps(Product.class);
    }

    public Optional<Product> findById(String id) {
        return Optional.ofNullable(elasticsearchOperations.get(id, Product.class, coordinates()));
    }

    public void save(Product product) {
        elasticsearchOperations.save(product, coordinates());
    }

    public void deleteById(String documentId) {
        try {
            elasticsearchOperations.delete(documentId, coordinates());
            log.info("Deleted product from index: id={}", documentId);
        } catch (NoSuchIndexException e) {
            log.debug("Index {} does not exist; delete is idempotent no-op for id={}",
                    Product.INDEX_NAME, documentId);
        }
    }
}
