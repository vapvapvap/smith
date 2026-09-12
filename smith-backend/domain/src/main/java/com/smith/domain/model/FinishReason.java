package com.smith.domain.model;

public enum FinishReason {
    STOP,
    LENGTH,
    CONTENT_FILTER,
    TOOL_CALLS,
    INSUFFICIENT_SYSTEM_RESOURCE,
    UNKNOWN;

    public static FinishReason fromValue(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        return switch (value) {
            case "stop" -> STOP;
            case "length" -> LENGTH;
            case "content_filter" -> CONTENT_FILTER;
            case "tool_calls" -> TOOL_CALLS;
            case "insufficient_system_resource" -> INSUFFICIENT_SYSTEM_RESOURCE;
            default -> UNKNOWN;
        };
    }
}
