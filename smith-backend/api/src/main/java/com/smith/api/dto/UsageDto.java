package com.smith.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Использование токенов")
public class UsageDto {

    @Schema(description = "Токены запроса")
    private Integer promptTokens;

    @Schema(description = "Токены ответа")
    private Integer completionTokens;

    @Schema(description = "Всего токенов")
    private Integer totalTokens;

    public UsageDto() {
    }

    public UsageDto(Integer promptTokens, Integer completionTokens, Integer totalTokens) {
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(Integer completionTokens) {
        this.completionTokens = completionTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }
}
