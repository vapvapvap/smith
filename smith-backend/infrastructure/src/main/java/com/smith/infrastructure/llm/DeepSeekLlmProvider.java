package com.smith.infrastructure.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.smith.domain.exception.LlmProviderException;
import com.smith.domain.model.ChatCompletion;
import com.smith.domain.model.ChatRequest;
import com.smith.domain.model.FinishReason;
import com.smith.domain.model.GenerationParams;
import com.smith.domain.model.ModelName;
import com.smith.domain.model.ReasoningEffort;
import com.smith.domain.model.ThinkingMode;
import com.smith.domain.model.Usage;
import com.smith.domain.port.ChatStreamListener;
import com.smith.domain.port.LlmProvider;
import com.smith.infrastructure.llm.dto.DeepSeekCompletionRequest;
import com.smith.infrastructure.llm.dto.DeepSeekCompletionResponse;
import com.smith.infrastructure.llm.dto.DeepSeekStreamChunk;
import com.smith.infrastructure.llm.dto.DeepSeekUsage;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

public class DeepSeekLlmProvider implements LlmProvider {

    private static final String DONE_MARKER = "[DONE]";
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final DeepSeekConfig config;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;

    public DeepSeekLlmProvider(DeepSeekConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(Timeout.of(config.connectTimeout()))
                        .setResponseTimeout(Timeout.of(config.readTimeout()))
                        .build())
                .build();
    }

    @Override
    public ChatCompletion complete(ChatRequest request) {
        HttpPost httpPost = new HttpPost(config.baseUrl() + CHAT_COMPLETIONS_PATH);
        httpPost.setHeader("Authorization", "Bearer " + config.apiKey());
        httpPost.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
        httpPost.setEntity(new StringEntity(toJson(buildRequest(request, false)),
                ContentType.APPLICATION_JSON, false));

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            int status = response.getCode();
            if (status >= 400) {
                throw new LlmProviderException("DeepSeek API error, status " + status
                        + ": " + EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
            }
            DeepSeekCompletionResponse body = objectMapper.readValue(
                    response.getEntity().getContent(), DeepSeekCompletionResponse.class);
            return mapCompletion(request, body);
        } catch (LlmProviderException e) {
            throw e;
        } catch (IOException | ParseException e) {
            throw new LlmProviderException("DeepSeek API call failed", e);
        }
    }

    @Override
    public void stream(ChatRequest request, ChatStreamListener listener) {
        HttpPost httpPost = new HttpPost(config.baseUrl() + CHAT_COMPLETIONS_PATH);
        httpPost.setHeader("Authorization", "Bearer " + config.apiKey());
        httpPost.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
        httpPost.setHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());
        httpPost.setEntity(new StringEntity(toJson(buildRequest(request, true)),
                ContentType.APPLICATION_JSON, false));

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            int status = response.getCode();
            if (status >= 400) {
                String errorBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                listener.onError(new LlmProviderException(
                        "DeepSeek API error, status " + status + ": " + errorBody));
                return;
            }
            FinishReason finishReason = null;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String payload = line.substring(5).trim();
                    if (payload.isEmpty()) {
                        continue;
                    }
                    if (payload.equals(DONE_MARKER)) {
                        break;
                    }
                    DeepSeekStreamChunk chunk = objectMapper.readValue(payload, DeepSeekStreamChunk.class);
                    if (chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
                        DeepSeekStreamChunk.Delta delta = chunk.getChoices().get(0).getDelta();
                        if (delta != null) {
                            listener.onChunk(delta.getContent(), delta.getReasoningContent());
                        }
                        if (chunk.getChoices().get(0).getFinishReason() != null) {
                            finishReason = FinishReason.fromValue(chunk.getChoices().get(0).getFinishReason());
                        }
                    }
                    if (chunk.getUsage() != null) {
                        listener.onUsage(toDomainUsage(chunk.getUsage()));
                    }
                }
            }
            listener.onComplete(finishReason == null ? FinishReason.UNKNOWN : finishReason);
        } catch (IOException | ParseException e) {
            listener.onError(new LlmProviderException("DeepSeek API stream failed", e));
        }
    }

    private DeepSeekCompletionRequest buildRequest(ChatRequest request, boolean stream) {
        DeepSeekCompletionRequest body = new DeepSeekCompletionRequest();
        body.setModel(request.model().value());
        body.setMessages(List.of(new DeepSeekCompletionRequest.Message("user", request.prompt().value())));
        GenerationParams params = request.params();
        body.setTemperature(params.temperature());
        body.setTopP(params.topP());
        body.setMaxTokens(params.maxTokens());
        body.setStop(params.stopSequences());
        body.setStream(stream);
        if (stream) {
            body.setStreamOptions(new DeepSeekCompletionRequest.StreamOptions(true));
        }
        ThinkingMode thinking = params.thinking() == null ? ThinkingMode.ENABLED : params.thinking();
        ReasoningEffort effort = params.reasoningEffort() == null ? ReasoningEffort.HIGH : params.reasoningEffort();
        body.setThinking(new DeepSeekCompletionRequest.Thinking(
                thinking == ThinkingMode.ENABLED ? "enabled" : "disabled", effort.name().toLowerCase()));
        return body;
    }

    private ChatCompletion mapCompletion(ChatRequest request, DeepSeekCompletionResponse body) {
        DeepSeekCompletionResponse.Message message = null;
        String finishReason = null;
        if (body.getChoices() != null && !body.getChoices().isEmpty()) {
            DeepSeekCompletionResponse.Choice choice = body.getChoices().get(0);
            message = choice.getMessage();
            finishReason = choice.getFinishReason();
        }
        ModelName model = body.getModel() == null ? request.model() : new ModelName(body.getModel());
        return new ChatCompletion(
                body.getId(),
                model,
                message == null ? null : message.getContent(),
                message == null ? null : message.getReasoningContent(),
                FinishReason.fromValue(finishReason),
                toDomainUsage(body.getUsage()),
                body.getCreated() == null ? Instant.now() : Instant.ofEpochSecond(body.getCreated()));
    }

    private Usage toDomainUsage(DeepSeekUsage usage) {
        if (usage == null) {
            return null;
        }
        return new Usage(usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
    }

    private String toJson(DeepSeekCompletionRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (IOException e) {
            throw new LlmProviderException("Failed to serialize DeepSeek request", e);
        }
    }
}
