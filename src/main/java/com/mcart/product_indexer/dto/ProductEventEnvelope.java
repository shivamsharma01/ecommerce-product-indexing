package com.mcart.product_indexer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Envelope for product events from product service outbox (Pub/Sub).
 * Format: { eventType, aggregateType, aggregateId, payload, occurredAt, version }
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductEventEnvelope {

    private String eventType;
    private String aggregateType;
    private String aggregateId;
    private JsonNode payload;  // Deserialized as tree; convert to ProductEventPayload in service
    private String occurredAt;
    private Integer version;
}
