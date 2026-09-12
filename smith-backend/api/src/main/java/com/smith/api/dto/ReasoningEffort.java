package com.smith.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ReasoningEffort {
    @JsonProperty("low") LOW,
    @JsonProperty("high") HIGH,
    @JsonProperty("max") MAX
}
