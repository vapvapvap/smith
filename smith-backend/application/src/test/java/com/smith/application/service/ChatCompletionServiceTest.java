package com.smith.application.service;

import com.smith.api.dto.ChatCompletionRequest;
import com.smith.api.dto.ChatCompletionResponse;
import com.smith.domain.model.ChatCompletion;
import com.smith.domain.model.ChatRecord;
import com.smith.domain.model.ChatRequest;
import com.smith.domain.model.ChatStatus;
import com.smith.domain.model.FinishReason;
import com.smith.domain.model.LlmModel;
import com.smith.domain.model.ModelName;
import com.smith.domain.model.ThinkingMode;
import com.smith.domain.model.Usage;
import com.smith.domain.port.ChatRequestRepository;
import com.smith.domain.port.LlmProvider;
import com.smith.domain.port.ModelRegistry;
import com.smith.domain.support.DefaultModelRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatCompletionServiceTest {

    private final LlmProvider provider = mock(LlmProvider.class);
    private final ChatRequestRepository repository = mock(ChatRequestRepository.class);
    private final ModelRegistry registry = new DefaultModelRegistry();
    private final ChatCompletionService service = new ChatCompletionService(provider, repository, registry);

    @Test
    void appliesDefaultGenerationParamsAndMapsResponse() {
        ChatCompletion completion = new ChatCompletion(
                "resp-1",
                ModelName.of(LlmModel.DEEPSEEK_V4_FLASH),
                "content",
                "reasoning",
                FinishReason.STOP,
                new Usage(1, 2, 3),
                Instant.parse("2026-01-01T00:00:00Z"));
        when(provider.complete(any(ChatRequest.class))).thenReturn(completion);

        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setPrompt("Hello");
        request.setModel("DEEPSEEK_V4_FLASH");

        ChatCompletionResponse response = service.complete(request);

        assertThat(response.getId()).isEqualTo("resp-1");
        assertThat(response.getModel()).isEqualTo("deepseek-v4-flash");
        assertThat(response.getContent()).isEqualTo("content");
        assertThat(response.getReasoningContent()).isEqualTo("reasoning");
        assertThat(response.getFinishReason()).isEqualTo("STOP");
        assertThat(response.getUsage().getTotalTokens()).isEqualTo(3);
        assertThat(response.getCreatedAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));

        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(provider).complete(captor.capture());
        ChatRequest built = captor.getValue();
        assertThat(built.model().value()).isEqualTo("deepseek-v4-flash");
        assertThat(built.prompt().value()).isEqualTo("Hello");
        assertThat(built.params().temperature()).isEqualTo(1.0);
        assertThat(built.params().topP()).isEqualTo(1.0);
        assertThat(built.params().presencePenalty()).isEqualTo(0.0);
        assertThat(built.params().frequencyPenalty()).isEqualTo(0.0);
        assertThat(built.params().thinking()).isEqualTo(ThinkingMode.ENABLED);
    }

    @Test
    void persistsCompletedRecord() {
        when(provider.complete(any(ChatRequest.class))).thenReturn(new ChatCompletion(
                "resp-1", ModelName.of(LlmModel.DEEPSEEK_V4_FLASH), "c", null,
                FinishReason.STOP, new Usage(1, 2, 3), Instant.now()));

        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setPrompt("Hello");
        request.setModel("DEEPSEEK_V4_FLASH");

        service.complete(request);

        ArgumentCaptor<ChatRecord> captor = ArgumentCaptor.forClass(ChatRecord.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(ChatStatus.COMPLETED);
        assertThat(captor.getValue().content()).isEqualTo("c");
    }
}
