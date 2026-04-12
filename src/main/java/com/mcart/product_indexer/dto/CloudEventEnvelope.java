package com.mcart.product_indexer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

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
