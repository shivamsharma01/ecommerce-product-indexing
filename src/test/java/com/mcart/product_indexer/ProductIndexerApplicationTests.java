package com.mcart.product_indexer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
class ProductIndexerApplicationTests {

	@Container
	static ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
			DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:7.17.21"))
			.withEnv("discovery.type", "single-node");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.elasticsearch.uris", () -> "http://" + elasticsearch.getHttpHostAddress());
		registry.add("product-indexer.pubsub.enabled", () -> "false");
		registry.add("spring.cloud.gcp.pubsub.enabled", () -> "false");
	}

	@Test
	void contextLoads() {
	}
}
