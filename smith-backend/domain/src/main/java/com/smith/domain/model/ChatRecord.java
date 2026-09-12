package com.smith.domain.model;

import java.time.Instant;
import java.util.UUID;

public record ChatRecord(
        UUID id,
        Prompt prompt,
        String systemPrompt,
        ModelName model,
        String content,
        String reasoningContent,
        FinishReason finishReason,
        Usage usage,
        boolean streamed,
        ChatStatus status,
        String errorMessage,
        Instant createdAt) {

    public static ChatRecord completed(
            ChatRequest request,
            ChatCompletion completion,
            Instant createdAt) {
        return new ChatRecord(
                request.id(),
                request.prompt(),
                request.systemPrompt(),
                completion.model(),
                completion.content(),
                completion.reasoningContent(),
                completion.finishReason(),
                completion.usage(),
                request.stream(),
                ChatStatus.COMPLETED,
                null,
                createdAt);
    }

    public static ChatRecord failed(ChatRequest request, String errorMessage, Instant createdAt) {
        return new ChatRecord(
                request.id(),
                request.prompt(),
                request.systemPrompt(),
                request.model(),
                null,
                null,
                null,
                null,
                request.stream(),
                ChatStatus.FAILED,
                errorMessage,
                createdAt);
    }
}
