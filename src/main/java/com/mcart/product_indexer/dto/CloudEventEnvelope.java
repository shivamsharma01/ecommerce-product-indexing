package com.mcart.product_indexer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CloudEvent envelope as delivered by Eventarc to Pub/Sub.
 * See: https://cloud.google.com/eventarc/docs/cloudevents-json
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CloudEventEnvelope {

    private String id;
    private String source;
    private String specversion;
    private String type;
    private String datacontenttype;
    private String subject;
    private String time;

    @JsonProperty("data")
    private DocumentEventData data;
}
