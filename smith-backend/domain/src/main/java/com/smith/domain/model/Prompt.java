package com.smith.domain.model;

public record Prompt(String value) {

    public Prompt {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Prompt must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
