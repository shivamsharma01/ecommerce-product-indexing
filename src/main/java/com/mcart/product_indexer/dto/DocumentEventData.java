package com.mcart.product_indexer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Firestore DocumentEventData from google.events.cloud.firestore.v1.
 * value: post-operation document (null for deletes)
 * old_value: pre-operation document (for updates/deletes)
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentEventData {

    private FirestoreDocument value;

    @JsonProperty("old_value")
    private FirestoreDocument oldValue;
}
