package com.smith.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Thinking {
    @JsonProperty("enabled") ENABLED,
    @JsonProperty("disabled") DISABLED
}
