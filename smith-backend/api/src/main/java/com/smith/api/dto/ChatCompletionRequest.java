package com.smith.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Запрос на генерацию ответа нейросетью")
public class ChatCompletionRequest {

    @Schema(description = "Текст запроса", example = "Расскажи про чистую архитектуру", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "prompt is required")
    private String prompt;

    @JsonProperty("system_prompt")
    @Schema(description = "Системный промпт (role=system), необязательный")
    private String systemPrompt;

    @Schema(description = "Алиас модели", example = "DEEPSEEK_V4_PRO", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "model is required")
    private String model;

    @Schema(description = "Температура выборки", example = "1.0", defaultValue = "1.0")
    private Double temperature;

    @JsonProperty("top_p")
    @Schema(description = "Nucleus sampling, диапазон (0, 1.0]", example = "1.0", defaultValue = "1.0")
    @DecimalMin(value = "0.0", inclusive = false, message = "top_p must be greater than 0")
    @DecimalMax(value = "1.0", message = "top_p must be at most 1.0")
    private Double topP;

    @JsonProperty("top_k")
    @Schema(description = "Принимается, но не передаётся в DeepSeek")
    private Integer topK;

    @JsonProperty("presence_penalty")
    @Schema(description = "Принимается, но не передаётся в DeepSeek (deprecated)", defaultValue = "0.0")
    private Double presencePenalty;

    @JsonProperty("frequency_penalty")
    @Schema(description = "Принимается, но не передаётся в DeepSeek (deprecated)", defaultValue = "0.0")
    private Double frequencyPenalty;

    @JsonProperty("max_tokens")
    @Schema(description = "Максимальное число токенов ответа")
    private Integer maxTokens;

    @JsonProperty("stop")
    @Schema(description = "Последовательности остановки генерации (до 16)")
    @Size(max = 16, message = "stop must contain at most 16 items")
    private List<String> stop;

    @Schema(description = "Режим ответа: false -> JSON, true -> SSE", defaultValue = "false")
    private Boolean stream;

    @Schema(description = "Режим thinking", defaultValue = "enabled")
    private Thinking thinking;

    @JsonProperty("reasoning_effort")
    @Schema(description = "Усилия рассуждения", defaultValue = "high")
    private ReasoningEffort reasoningEffort;

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    public Double getPresencePenalty() {
        return presencePenalty;
    }

    public void setPresencePenalty(Double presencePenalty) {
        this.presencePenalty = presencePenalty;
    }

    public Double getFrequencyPenalty() {
        return frequencyPenalty;
    }

    public void setFrequencyPenalty(Double frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public List<String> getStop() {
        return stop;
    }

    public void setStop(List<String> stop) {
        this.stop = stop;
    }

    public Boolean getStream() {
        return stream;
    }

    public void setStream(Boolean stream) {
        this.stream = stream;
    }

    public Thinking getThinking() {
        return thinking;
    }

    public void setThinking(Thinking thinking) {
        this.thinking = thinking;
    }

    public ReasoningEffort getReasoningEffort() {
        return reasoningEffort;
    }

    public void setReasoningEffort(ReasoningEffort reasoningEffort) {
        this.reasoningEffort = reasoningEffort;
    }
}
