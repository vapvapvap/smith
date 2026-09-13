package com.smith.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Обновлённые факты диалога")
public class FactsResponse {

    @Schema(description = "Факты в формате «ключ: значение» (по одному на строку)")
    private String facts;

    @Schema(description = "Использование токенов")
    private UsageDto usage;

    public FactsResponse() {
    }

    public FactsResponse(String facts, UsageDto usage) {
        this.facts = facts;
        this.usage = usage;
    }

    public String getFacts() {
        return facts;
    }

    public void setFacts(String facts) {
        this.facts = facts;
    }

    public UsageDto getUsage() {
        return usage;
    }

    public void setUsage(UsageDto usage) {
        this.usage = usage;
    }
}
