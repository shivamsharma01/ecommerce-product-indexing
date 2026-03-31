package com.mcart.product_indexer;

import com.google.cloud.spring.autoconfigure.firestore.FirestoreTransactionManagerAutoConfiguration;
import com.google.cloud.spring.data.firestore.repository.config.EnableReactiveFirestoreRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ReactiveElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;

@SpringBootApplication(exclude = {
		FirestoreTransactionManagerAutoConfiguration.class,
		ElasticsearchClientAutoConfiguration.class,
		ElasticsearchRestClientAutoConfiguration.class,
		ElasticsearchDataAutoConfiguration.class,
		ElasticsearchRepositoriesAutoConfiguration.class,
		ReactiveElasticsearchRepositoriesAutoConfiguration.class
})
@EnableReactiveFirestoreRepositories
public class ProductIndexerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductIndexerApplication.class, args);
	}

}
