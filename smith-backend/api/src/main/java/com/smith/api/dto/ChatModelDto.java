package com.smith.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Информация о доступной модели")
public class ChatModelDto {

    @Schema(description = "Алиас модели для запросов", example = "DEEPSEEK_V4_PRO")
    private String alias;

    @Schema(description = "Имя модели у провайдера", example = "deepseek-v4-pro")
    private String providerName;

    @Schema(description = "Поддерживает ли модель изображения (vision)", example = "false")
    private boolean vision;

    public ChatModelDto() {
    }

    public ChatModelDto(String alias, String providerName, boolean vision) {
        this.alias = alias;
        this.providerName = providerName;
        this.vision = vision;
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

    public boolean isVision() {
        return vision;
    }

    public void setVision(boolean vision) {
        this.vision = vision;
    }
}
