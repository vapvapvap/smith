package com.smith.application.service;

import com.smith.api.dto.AttachmentDto;
import com.smith.api.dto.ChatCompletionRequest;
import com.smith.api.dto.ChatCompletionResponse;
import com.smith.api.dto.FactsRequest;
import com.smith.api.dto.FactsResponse;
import com.smith.api.dto.SummarizeRequest;
import com.smith.api.dto.SummarizeResponse;
import com.smith.domain.exception.UnsupportedAttachmentException;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
                ModelName.of(LlmModel.DEEPSEEK_FLASH),
                "content",
                "reasoning",
                FinishReason.STOP,
                new Usage(1, 2, 3),
                Instant.parse("2026-01-01T00:00:00Z"));
        when(provider.complete(any(ChatRequest.class))).thenReturn(completion);

        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setPrompt("Hello");
        request.setModel("DEEPSEEK_FLASH");
        request.setSystemPrompt("Ты — полезный ассистент");

        ChatCompletionResponse response = service.complete(request);

        assertThat(response.getId()).isEqualTo("resp-1");
        assertThat(response.getModel()).isEqualTo("deepseek-flash");
        assertThat(response.getContent()).isEqualTo("content");
        assertThat(response.getReasoningContent()).isEqualTo("reasoning");
        assertThat(response.getFinishReason()).isEqualTo("STOP");
        assertThat(response.getUsage().getTotalTokens()).isEqualTo(3);
        assertThat(response.getCreatedAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));

        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(provider).complete(captor.capture());
        ChatRequest built = captor.getValue();
        assertThat(built.model().value()).isEqualTo("deepseek-flash");
        assertThat(built.prompt().value()).isEqualTo("Hello");
        assertThat(built.systemPrompt()).isEqualTo("Ты — полезный ассистент");
        assertThat(built.params().temperature()).isEqualTo(1.0);
        assertThat(built.params().topP()).isEqualTo(1.0);
        assertThat(built.params().presencePenalty()).isEqualTo(0.0);
        assertThat(built.params().frequencyPenalty()).isEqualTo(0.0);
        assertThat(built.params().thinking()).isEqualTo(ThinkingMode.ENABLED);
    }

    @Test
    void persistsCompletedRecord() {
        when(provider.complete(any(ChatRequest.class))).thenReturn(new ChatCompletion(
                "resp-1", ModelName.of(LlmModel.DEEPSEEK_FLASH), "c", null,
                FinishReason.STOP, new Usage(1, 2, 3), Instant.now()));

        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setPrompt("Hello");
        request.setModel("DEEPSEEK_FLASH");

        service.complete(request);

        ArgumentCaptor<ChatRecord> captor = ArgumentCaptor.forClass(ChatRecord.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(ChatStatus.COMPLETED);
        assertThat(captor.getValue().content()).isEqualTo("c");
    }

    @Test
    void mapsImageAttachmentsForVisionModel() {
        when(provider.complete(any(ChatRequest.class))).thenReturn(new ChatCompletion(
                "resp-1", ModelName.of(LlmModel.DEEPSEEK_FLASH), "c", null,
                FinishReason.STOP, new Usage(1, 2, 3), Instant.now()));

        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setPrompt("Что на картинке?");
        request.setModel("DEEPSEEK_FLASH");
        AttachmentDto attachment = new AttachmentDto();
        attachment.setName("photo.png");
        attachment.setMimeType("image/png");
        attachment.setData("aGVsbG8=");
        request.setAttachments(List.of(attachment));

        service.complete(request);

        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(provider).complete(captor.capture());
        ChatRequest built = captor.getValue();
        assertThat(built.attachments()).hasSize(1);
        assertThat(built.attachments().get(0).mimeType()).isEqualTo("image/png");
        assertThat(built.attachments().get(0).dataUri()).isEqualTo("data:image/png;base64,aGVsbG8=");
    }

    @Test
    void rejectsImageAttachmentForNonVisionModel() {
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setPrompt("Что на картинке?");
        request.setModel("DEEPSEEK_V4_PRO");
        AttachmentDto attachment = new AttachmentDto();
        attachment.setName("photo.png");
        attachment.setMimeType("image/png");
        attachment.setData("aGVsbG8=");
        request.setAttachments(List.of(attachment));

        assertThatThrownBy(() -> service.complete(request))
                .isInstanceOf(UnsupportedAttachmentException.class);
    }

    @Test
    void summarizeDoesNotPersistAndUsesSummarizePrompt() {
        when(provider.complete(any(ChatRequest.class))).thenReturn(new ChatCompletion(
                "resp-1", ModelName.of(LlmModel.DEEPSEEK_FLASH), "краткое саммари", null,
                FinishReason.STOP, new Usage(5, 7, 12), Instant.now()));

        SummarizeRequest request = new SummarizeRequest();
        request.setText("Пользователь: привет\n\nАссистент: привет");
        request.setModel("DEEPSEEK_FLASH");

        SummarizeResponse response = service.summarize(request);

        assertThat(response.getSummary()).isEqualTo("краткое саммари");
        assertThat(response.getUsage().getTotalTokens()).isEqualTo(12);

        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(provider).complete(captor.capture());
        ChatRequest built = captor.getValue();
        assertThat(built.prompt().value()).contains("привет");
        assertThat(built.systemPrompt()).isNotBlank();
        assertThat(built.params().thinking()).isEqualTo(ThinkingMode.DISABLED);
        assertThat(built.attachments()).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    void factsDoesNotPersistAndMergesExistingFacts() {
        when(provider.complete(any(ChatRequest.class))).thenReturn(new ChatCompletion(
                "resp-1", ModelName.of(LlmModel.DEEPSEEK_FLASH), "цель: собрать ТЗ", null,
                FinishReason.STOP, new Usage(9, 4, 13), Instant.now()));

        FactsRequest request = new FactsRequest();
        request.setText("Пользователь: нужен план проекта");
        request.setFacts("заказчик: ООО Ромашка");
        request.setModel("DEEPSEEK_FLASH");

        FactsResponse response = service.facts(request);

        assertThat(response.getFacts()).isEqualTo("цель: собрать ТЗ");
        assertThat(response.getUsage().getTotalTokens()).isEqualTo(13);

        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(provider).complete(captor.capture());
        ChatRequest built = captor.getValue();
        assertThat(built.prompt().value()).contains("заказчик: ООО Ромашка");
        assertThat(built.prompt().value()).contains("нужен план проекта");
        assertThat(built.systemPrompt()).isNotBlank();
        assertThat(built.params().thinking()).isEqualTo(ThinkingMode.DISABLED);
        assertThat(built.attachments()).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsNonImageAttachment() {
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setPrompt("Прочитай файл");
        request.setModel("DEEPSEEK_FLASH");
        AttachmentDto attachment = new AttachmentDto();
        attachment.setName("notes.txt");
        attachment.setMimeType("text/plain");
        attachment.setData("aGVsbG8=");
        request.setAttachments(List.of(attachment));

        assertThatThrownBy(() -> service.complete(request))
                .isInstanceOf(UnsupportedAttachmentException.class);
    }
}
