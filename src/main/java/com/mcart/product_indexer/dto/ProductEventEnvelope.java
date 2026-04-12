package com.mcart.product_indexer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductEventEnvelope {

    private String eventType;
    private String aggregateType;
    private String aggregateId;
    private JsonNode payload;
    private String occurredAt;
    private Integer version;
}
