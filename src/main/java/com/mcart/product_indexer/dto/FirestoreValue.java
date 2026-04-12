package com.mcart.product_indexer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FirestoreValue {

    @JsonProperty("string_value")
    private String stringValue;

    @JsonProperty("integer_value")
    private String integerValue;

    @JsonProperty("double_value")
    private Double doubleValue;

    @JsonProperty("boolean_value")
    private Boolean booleanValue;

    @JsonProperty("null_value")
    private Object nullValue;

    @JsonProperty("array_value")
    private ArrayValue arrayValue;

    @JsonProperty("map_value")
    private MapValue mapValue;

    public String getString() {
        return stringValue;
    }

    public Long getLong() {
        return integerValue != null ? Long.parseLong(integerValue) : null;
    }

    public Integer getInteger() {
        return integerValue != null ? Integer.parseInt(integerValue) : null;
    }

    public Double getDouble() {
        return doubleValue;
    }

    public Boolean getBoolean() {
        return booleanValue;
    }

    public boolean isNull() {
        return nullValue != null;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ArrayValue {
        @JsonProperty("values")
        private List<FirestoreValue> values;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MapValue {
        @JsonProperty("fields")
        private Map<String, FirestoreValue> fields;
    }
}
