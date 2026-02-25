package com.mcart.product_indexer;

import com.google.cloud.spring.autoconfigure.firestore.FirestoreTransactionManagerAutoConfiguration;
import com.google.cloud.spring.data.firestore.repository.config.EnableReactiveFirestoreRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = FirestoreTransactionManagerAutoConfiguration.class)
@EnableReactiveFirestoreRepositories
public class ProductIndexerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductIndexerApplication.class, args);
	}

}
