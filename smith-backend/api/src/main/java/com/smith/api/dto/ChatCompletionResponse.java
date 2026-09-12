package com.smith.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Ответ на генерацию")
public class ChatCompletionResponse {

    @Schema(description = "Идентификатор ответа провайдера")
    private String id;

    @Schema(description = "Имя модели провайдера")
    private String model;

    @Schema(description = "Содержимое ответа")
    private String content;

    @Schema(description = "Содержимое рассуждений (thinking mode)")
    private String reasoningContent;

    @Schema(description = "Причина завершения генерации")
    private String finishReason;

    @Schema(description = "Использование токенов")
    private UsageDto usage;

    @Schema(description = "Время создания ответа")
    private Instant createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getReasoningContent() {
        return reasoningContent;
    }

    public void setReasoningContent(String reasoningContent) {
        this.reasoningContent = reasoningContent;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    public UsageDto getUsage() {
        return usage;
    }

    public void setUsage(UsageDto usage) {
        this.usage = usage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
