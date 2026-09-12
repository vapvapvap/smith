package com.smith.domain.port;

import com.smith.domain.model.LlmModel;
import com.smith.domain.exception.UnknownModelException;

import java.util.List;

public interface ModelRegistry {

    LlmModel resolve(String alias) throws UnknownModelException;

    List<LlmModel> availableModels();
}
