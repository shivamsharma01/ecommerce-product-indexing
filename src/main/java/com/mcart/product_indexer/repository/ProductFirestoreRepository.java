package com.mcart.product_indexer.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.mcart.product_indexer.model.ProductFirestoreDocument;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductFirestoreRepository extends FirestoreReactiveRepository<ProductFirestoreDocument> {
}
