package com.smith.domain.port;

import com.smith.domain.model.ChatCompletion;
import com.smith.domain.model.ChatRequest;

public interface LlmProvider {

    ChatCompletion complete(ChatRequest request);

    void stream(ChatRequest request, ChatStreamListener listener);
}
