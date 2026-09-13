package com.smith.domain.model;

public enum LlmModel {

    DEEPSEEK_V4_PRO("deepseek-v4-pro", false),
    DEEPSEEK_FLASH("deepseek-flash", true);

    private final String providerName;
    private final boolean supportsVision;

    LlmModel(String providerName, boolean supportsVision) {
        this.providerName = providerName;
        this.supportsVision = supportsVision;
    }

    public String providerName() {
        return providerName;
    }

    public boolean supportsVision() {
        return supportsVision;
    }
}
