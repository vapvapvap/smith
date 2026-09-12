package com.smith.domain.support;

import com.smith.domain.exception.UnknownModelException;
import com.smith.domain.model.LlmModel;
import com.smith.domain.port.ModelRegistry;

import java.util.Arrays;
import java.util.List;

public class DefaultModelRegistry implements ModelRegistry {

    @Override
    public LlmModel resolve(String alias) throws UnknownModelException {
        for (LlmModel model : LlmModel.values()) {
            if (model.name().equalsIgnoreCase(alias)) {
                return model;
            }
        }
        throw new UnknownModelException(alias, availableAliases());
    }

    @Override
    public List<LlmModel> availableModels() {
        return Arrays.asList(LlmModel.values());
    }

    private List<String> availableAliases() {
        return Arrays.stream(LlmModel.values()).map(Enum::name).toList();
    }
}
