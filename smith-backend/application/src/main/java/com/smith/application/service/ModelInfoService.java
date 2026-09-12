package com.smith.application.service;

import com.smith.api.dto.ChatModelDto;
import com.smith.domain.model.LlmModel;
import com.smith.domain.port.ModelRegistry;

import java.util.List;

public class ModelInfoService {

    private final ModelRegistry modelRegistry;

    public ModelInfoService(ModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry;
    }

    public List<ChatModelDto> availableModels() {
        return modelRegistry.availableModels().stream()
                .map(model -> new ChatModelDto(model.name(), model.providerName()))
                .toList();
    }
}
