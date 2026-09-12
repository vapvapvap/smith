package com.smith.domain.model;

public record Usage(
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens) {
}
