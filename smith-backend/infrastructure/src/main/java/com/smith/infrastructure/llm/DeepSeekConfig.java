package com.smith.infrastructure.llm;

import java.time.Duration;

public record DeepSeekConfig(
        String baseUrl,
        String apiKey,
        Duration connectTimeout,
        Duration readTimeout) {

    public DeepSeekConfig {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl must not be blank");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("apiKey must not be blank");
        }
    }
}
