package com.smith.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на саммаризацию контекста диалога")
public class SummarizeRequest {

    @Schema(description = "Текст диалога для сжатия", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "text is required")
    private String text;

    @Schema(description = "Алиас модели", example = "DEEPSEEK_FLASH", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "model is required")
    private String model;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
