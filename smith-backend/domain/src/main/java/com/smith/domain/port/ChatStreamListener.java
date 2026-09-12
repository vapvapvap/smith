package com.smith.domain.port;

import com.smith.domain.exception.LlmProviderException;
import com.smith.domain.model.FinishReason;
import com.smith.domain.model.Usage;

public interface ChatStreamListener {

    void onChunk(String contentDelta, String reasoningDelta);

    void onUsage(Usage usage);

    void onComplete(FinishReason finishReason);

    void onError(LlmProviderException error);
}
