package com.smith.domain.model;

import java.util.Objects;

public record ModelName(String value) {

    public ModelName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Model name must not be blank");
        }
    }

    public static ModelName of(LlmModel model) {
        Objects.requireNonNull(model, "model must not be null");
        return new ModelName(model.providerName());
    }

    @Override
    public String toString() {
        return value;
    }
}
