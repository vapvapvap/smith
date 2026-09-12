package com.smith.domain.model;

import java.util.List;
import java.util.UUID;

public record ChatRequest(
        UUID id,
        Prompt prompt,
        ModelName model,
        GenerationParams params,
        boolean stream,
        String systemPrompt,
        List<Attachment> attachments) {

    public ChatRequest {
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }

    public static ChatRequest of(Prompt prompt, ModelName model, GenerationParams params, boolean stream,
                                 String systemPrompt, List<Attachment> attachments) {
        return new ChatRequest(UUID.randomUUID(), prompt, model, params, stream, systemPrompt, attachments);
    }
}
