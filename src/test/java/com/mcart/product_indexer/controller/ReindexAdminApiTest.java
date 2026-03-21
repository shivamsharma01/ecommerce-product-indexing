package com.mcart.product_indexer.controller;

import com.mcart.product_indexer.service.ReindexResult;
import com.mcart.product_indexer.service.ReindexService;
import com.mcart.product_indexer.support.IntegrationTestJwtConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTestJwtConfig.class)
@TestPropertySource(properties = {
        "product-indexer.pubsub.enabled=false",
        "spring.cloud.gcp.pubsub.enabled=false"
})
class ReindexAdminApiTest {

    @Container
    static ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:7.17.21"))
            .withEnv("discovery.type", "single-node");

    @DynamicPropertySource
    static void esProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", () -> "http://" + elasticsearch.getHttpHostAddress());
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReindexService reindexService;

    @BeforeEach
    void stubReindex() {
        when(reindexService.reindex()).thenReturn(new ReindexResult(3, 1));
    }

    @Test
    void reindex_withoutBearerToken_returns401() throws Exception {
        mockMvc.perform(post("/product-indexer/admin/reindex"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reindex_withBearerToken_returnsBody() throws Exception {
        mockMvc.perform(post("/product-indexer/admin/reindex")
                        .contentType(APPLICATION_JSON)
                        .header("Authorization", "Bearer test.jwt.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.indexedCount").value(3))
                .andExpect(jsonPath("$.failedCount").value(1));
    }
}
