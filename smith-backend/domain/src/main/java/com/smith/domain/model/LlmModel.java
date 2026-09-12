package com.smith.domain.model;

public enum LlmModel {

    DEEPSEEK_V4_PRO("deepseek-v4-pro"),
    DEEPSEEK_V4_FLASH("deepseek-v4-flash"),
    DEEPSEEK_V4_FLASH_VISION_EXP("deepseek-v4-flash-vision-exp");

    private final String providerName;

    LlmModel(String providerName) {
        this.providerName = providerName;
    }

    public String providerName() {
        return providerName;
    }
}
