package com.smith.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Результат саммаризации контекста диалога")
public class SummarizeResponse {

    @Schema(description = "Текст саммари")
    private String summary;

    @Schema(description = "Использование токенов")
    private UsageDto usage;

    public SummarizeResponse() {
    }

    public SummarizeResponse(String summary, UsageDto usage) {
        this.summary = summary;
        this.usage = usage;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public UsageDto getUsage() {
        return usage;
    }

    public void setUsage(UsageDto usage) {
        this.usage = usage;
    }
}
