package com.smith.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Информация о доступной модели")
public class ChatModelDto {

    @Schema(description = "Алиас модели для запросов", example = "DEEPSEEK_V4_PRO")
    private String alias;

    @Schema(description = "Имя модели у провайдера", example = "deepseek-v4-pro")
    private String providerName;

    public ChatModelDto() {
    }

    public ChatModelDto(String alias, String providerName) {
        this.alias = alias;
        this.providerName = providerName;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }
}
