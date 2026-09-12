package com.smith.domain.model;

import java.util.List;

public record GenerationParams(
        Double temperature,
        Double topP,
        Integer topK,
        Double presencePenalty,
        Double frequencyPenalty,
        Integer maxTokens,
        List<String> stopSequences,
        ThinkingMode thinking,
        ReasoningEffort reasoningEffort) {

    public GenerationParams {
        if (stopSequences != null && stopSequences.size() > 16) {
            throw new IllegalArgumentException("stopSequences must contain at most 16 items");
        }
    }
}
