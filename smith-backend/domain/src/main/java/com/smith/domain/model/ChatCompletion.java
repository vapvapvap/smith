package com.smith.domain.model;

import java.time.Instant;

public record ChatCompletion(
        String id,
        ModelName model,
        String content,
        String reasoningContent,
        FinishReason finishReason,
        Usage usage,
        Instant createdAt) {
}
