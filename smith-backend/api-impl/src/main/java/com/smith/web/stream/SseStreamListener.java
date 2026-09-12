package com.smith.web.stream;

import com.smith.domain.exception.LlmProviderException;
import com.smith.domain.model.FinishReason;
import com.smith.domain.model.Usage;
import com.smith.domain.port.ChatStreamListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

public class SseStreamListener implements ChatStreamListener {

    private static final String EVENT_CHUNK = "chunk";
    private static final String EVENT_USAGE = "usage";
    private static final String EVENT_DONE = "done";
    private static final String EVENT_ERROR = "error";

    private final SseEmitter emitter;

    public SseStreamListener(SseEmitter emitter) {
        this.emitter = emitter;
    }

    @Override
    public void onChunk(String contentDelta, String reasoningDelta) {
        send(EVENT_CHUNK, Map.of(
                "content", contentDelta == null ? "" : contentDelta,
                "reasoningContent", reasoningDelta == null ? "" : reasoningDelta));
    }

    @Override
    public void onUsage(Usage usage) {
        send(EVENT_USAGE, usage);
    }

    @Override
    public void onComplete(FinishReason finishReason) {
        send(EVENT_DONE, Map.of("finishReason", finishReason.name()));
        emitter.complete();
    }

    @Override
    public void onError(LlmProviderException error) {
        try {
            send(EVENT_ERROR, Map.of("message", error.getMessage()));
        } finally {
            emitter.completeWithError(error);
        }
    }

    private void send(String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}
