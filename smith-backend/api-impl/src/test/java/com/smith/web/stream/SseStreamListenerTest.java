package com.smith.web.stream;

import com.smith.domain.exception.LlmProviderException;
import com.smith.domain.model.FinishReason;
import com.smith.domain.model.Usage;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class SseStreamListenerTest {

    private final SseEmitter emitter = mock(SseEmitter.class);
    private final SseStreamListener listener = new SseStreamListener(emitter);

    @Test
    void chunkIsEmittedAsSseEvent() throws Exception {
        listener.onChunk("hello", "reason");
        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void completionCompletesEmitter() throws Exception {
        listener.onUsage(new Usage(1, 2, 3));
        listener.onComplete(FinishReason.STOP);

        verify(emitter, times(2)).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter).complete();
    }

    @Test
    void errorCompletesEmitterWithError() throws Exception {
        listener.onError(new LlmProviderException("boom"));

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter).completeWithError(any(LlmProviderException.class));
    }

    @Test
    void noFurtherCallsAfterError() throws Exception {
        listener.onError(new LlmProviderException("boom"));

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter).completeWithError(any(LlmProviderException.class));
        verifyNoMoreInteractions(emitter);
    }
}
