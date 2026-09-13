package com.smith.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на извлечение и обновление фактов диалога (key-value память)")
public class FactsRequest {

    @Schema(description = "Текст новых сообщений диалога для извлечения фактов", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "text is required")
    private String text;

    @Schema(description = "Текущие факты в формате «ключ: значение» (по одному на строку)")
    private String facts;

    @Schema(description = "Алиас модели", example = "DEEPSEEK_FLASH", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "model is required")
    private String model;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getFacts() {
        return facts;
    }

    public void setFacts(String facts) {
        this.facts = facts;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
