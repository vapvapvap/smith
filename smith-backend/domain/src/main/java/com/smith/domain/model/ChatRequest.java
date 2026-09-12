package com.smith.domain.model;

import java.time.Instant;
import java.util.UUID;

public record ChatRequest(
        UUID id,
        Prompt prompt,
        ModelName model,
        GenerationParams params,
        boolean stream,
        String systemPrompt) {

    public static ChatRequest of(Prompt prompt, ModelName model, GenerationParams params, boolean stream,
                                 String systemPrompt) {
        return new ChatRequest(UUID.randomUUID(), prompt, model, params, stream, systemPrompt);
    }
}
