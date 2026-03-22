package com.mcart.product_indexer;

import com.mcart.product_indexer.elasticsearch.ProductElasticsearchIndex;
import com.mcart.product_indexer.repository.ProductFirestoreRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ProductIndexerApplicationTests {

	@MockBean
	private ProductElasticsearchIndex productElasticsearchIndex;

	@MockBean
	private ProductFirestoreRepository productFirestoreRepository;

	@Test
	void contextLoads() {
	}
}
