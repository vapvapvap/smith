package com.smith.application.service;

import com.smith.api.dto.AttachmentDto;
import com.smith.api.dto.ChatCompletionRequest;
import com.smith.api.dto.ChatCompletionResponse;
import com.smith.api.dto.ReasoningEffort;
import com.smith.api.dto.SummarizeRequest;
import com.smith.api.dto.SummarizeResponse;
import com.smith.api.dto.Thinking;
import com.smith.api.dto.UsageDto;
import com.smith.domain.exception.LlmProviderException;
import com.smith.domain.exception.UnknownModelException;
import com.smith.domain.exception.UnsupportedAttachmentException;
import com.smith.domain.model.Attachment;
import com.smith.domain.model.ChatCompletion;
import com.smith.domain.model.ChatRecord;
import com.smith.domain.model.ChatRequest;
import com.smith.domain.model.FinishReason;
import com.smith.domain.model.GenerationParams;
import com.smith.domain.model.LlmModel;
import com.smith.domain.model.ModelName;
import com.smith.domain.model.Prompt;
import com.smith.domain.model.Usage;
import com.smith.domain.port.ChatRequestRepository;
import com.smith.domain.port.ChatStreamListener;
import com.smith.domain.port.LlmProvider;
import com.smith.domain.port.ModelRegistry;

import java.time.Instant;
import java.util.List;

public class ChatCompletionService {

    private static final double DEFAULT_TEMPERATURE = 1.0;
    private static final double DEFAULT_TOP_P = 1.0;
    private static final double DEFAULT_PENALTY = 0.0;
    private static final boolean DEFAULT_STREAM = false;
    private static final String SUMMARIZE_SYSTEM_PROMPT = """
            Ты — система сжатия истории диалога. Составь краткое, но содержательное саммари
            приведённого диалога. Сохрани ключевые факты, имена, числа, принятые решения,
            договорённости и незавершённые задачи. Не добавляй новых сведений и не давай
            оценок. Пиши на языке диалога.""";

    private final LlmProvider llmProvider;
    private final ChatRequestRepository repository;
    private final ModelRegistry modelRegistry;

    public ChatCompletionService(LlmProvider llmProvider,
                                 ChatRequestRepository repository,
                                 ModelRegistry modelRegistry) {
        this.llmProvider = llmProvider;
        this.repository = repository;
        this.modelRegistry = modelRegistry;
    }

    public ChatCompletionResponse complete(ChatCompletionRequest request) {
        ChatRequest chatRequest = buildChatRequest(request);
        try {
            ChatCompletion completion = llmProvider.complete(chatRequest);
            repository.save(ChatRecord.completed(chatRequest, completion, Instant.now()));
            return toResponse(completion);
        } catch (LlmProviderException | UnknownModelException e) {
            repository.save(ChatRecord.failed(chatRequest, e.getMessage(), Instant.now()));
            throw e;
        }
    }

    public void stream(ChatCompletionRequest request, ChatStreamListener listener) {
        ChatRequest chatRequest = buildChatRequest(request);
        StringBuilder content = new StringBuilder();
        StringBuilder reasoning = new StringBuilder();
        Usage[] usageHolder = new Usage[1];
        FinishReason[] finishReasonHolder = new FinishReason[1];
        Instant start = Instant.now();

        llmProvider.stream(chatRequest, new ChatStreamListener() {
            @Override
            public void onChunk(String contentDelta, String reasoningDelta) {
                if (contentDelta != null) {
                    content.append(contentDelta);
                }
                if (reasoningDelta != null) {
                    reasoning.append(reasoningDelta);
                }
                listener.onChunk(contentDelta, reasoningDelta);
            }

            @Override
            public void onUsage(Usage usage) {
                usageHolder[0] = usage;
                listener.onUsage(usage);
            }

            @Override
            public void onComplete(FinishReason finishReason) {
                finishReasonHolder[0] = finishReason;
                ChatCompletion completion = new ChatCompletion(
                        null,
                        chatRequest.model(),
                        content.isEmpty() ? null : content.toString(),
                        reasoning.isEmpty() ? null : reasoning.toString(),
                        finishReason,
                        usageHolder[0],
                        Instant.now());
                repository.save(ChatRecord.completed(chatRequest, completion, start));
                listener.onComplete(finishReason);
            }

            @Override
            public void onError(LlmProviderException error) {
                repository.save(ChatRecord.failed(chatRequest, error.getMessage(), start));
                listener.onError(error);
            }
        });
    }

    public SummarizeResponse summarize(SummarizeRequest request) {
        LlmModel model = modelRegistry.resolve(request.getModel());
        GenerationParams params = new GenerationParams(
                DEFAULT_TEMPERATURE,
                DEFAULT_TOP_P,
                null,
                DEFAULT_PENALTY,
                DEFAULT_PENALTY,
                null,
                null,
                com.smith.domain.model.ThinkingMode.DISABLED,
                com.smith.domain.model.ReasoningEffort.HIGH);
        ChatRequest chatRequest = ChatRequest.of(
                new Prompt(request.getText()),
                ModelName.of(model),
                params,
                false,
                SUMMARIZE_SYSTEM_PROMPT,
                List.of());
        ChatCompletion completion = llmProvider.complete(chatRequest);
        UsageDto usage = completion.usage() == null ? null : new UsageDto(
                completion.usage().promptTokens(),
                completion.usage().completionTokens(),
                completion.usage().totalTokens());
        return new SummarizeResponse(completion.content(), usage);
    }

    private ChatRequest buildChatRequest(ChatCompletionRequest request) {
        LlmModel model = modelRegistry.resolve(request.getModel());
        List<Attachment> attachments = mapAttachments(request.getAttachments());
        if (!attachments.isEmpty() && !model.supportsVision()) {
            throw new UnsupportedAttachmentException(
                    "Model " + model.name() + " does not support image attachments");
        }
        boolean stream = request.getStream() != null ? request.getStream() : DEFAULT_STREAM;
        GenerationParams params = new GenerationParams(
                request.getTemperature() != null ? request.getTemperature() : DEFAULT_TEMPERATURE,
                request.getTopP() != null ? request.getTopP() : DEFAULT_TOP_P,
                request.getTopK(),
                request.getPresencePenalty() != null ? request.getPresencePenalty() : DEFAULT_PENALTY,
                request.getFrequencyPenalty() != null ? request.getFrequencyPenalty() : DEFAULT_PENALTY,
                request.getMaxTokens(),
                request.getStop(),
                mapThinking(request.getThinking()),
                mapReasoningEffort(request.getReasoningEffort()));
        return ChatRequest.of(
                new Prompt(request.getPrompt()),
                ModelName.of(model),
                params,
                stream,
                request.getSystemPrompt(),
                attachments);
    }

    private List<Attachment> mapAttachments(List<AttachmentDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return List.of();
        }
        return dtos.stream().map(dto -> {
            String mimeType = dto.getMimeType();
            if (mimeType == null || !mimeType.startsWith("image/")) {
                throw new UnsupportedAttachmentException(
                        "Only image attachments are supported, got: " + mimeType);
            }
            if (dto.getData() == null || dto.getData().isBlank()) {
                throw new UnsupportedAttachmentException("Attachment data must not be blank");
            }
            return new Attachment(dto.getName(), mimeType, dto.getData());
        }).toList();
    }

    private com.smith.domain.model.ThinkingMode mapThinking(Thinking thinking) {
        if (thinking == null) {
            return com.smith.domain.model.ThinkingMode.ENABLED;
        }
        return thinking == Thinking.ENABLED
                ? com.smith.domain.model.ThinkingMode.ENABLED
                : com.smith.domain.model.ThinkingMode.DISABLED;
    }

    private com.smith.domain.model.ReasoningEffort mapReasoningEffort(ReasoningEffort effort) {
        if (effort == null) {
            return com.smith.domain.model.ReasoningEffort.HIGH;
        }
        return switch (effort) {
            case LOW -> com.smith.domain.model.ReasoningEffort.LOW;
            case MAX -> com.smith.domain.model.ReasoningEffort.MAX;
            case HIGH -> com.smith.domain.model.ReasoningEffort.HIGH;
        };
    }

    private ChatCompletionResponse toResponse(ChatCompletion completion) {
        ChatCompletionResponse response = new ChatCompletionResponse();
        response.setId(completion.id());
        response.setModel(completion.model() == null ? null : completion.model().value());
        response.setContent(completion.content());
        response.setReasoningContent(completion.reasoningContent());
        response.setFinishReason(completion.finishReason() == null ? null : completion.finishReason().name());
        if (completion.usage() != null) {
            response.setUsage(new UsageDto(
                    completion.usage().promptTokens(),
                    completion.usage().completionTokens(),
                    completion.usage().totalTokens()));
        }
        response.setCreatedAt(completion.createdAt());
        return response;
    }
}
