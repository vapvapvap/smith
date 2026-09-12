package com.smith.infrastructure.llm.dto;

import java.util.List;

public class DeepSeekCompletionRequest {

    private String model;
    private List<Message> messages;
    private Double temperature;
    private Double topP;
    private Integer maxTokens;
    private List<String> stop;
    private boolean stream;
    private StreamOptions streamOptions;
    private Thinking thinking;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
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

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public StreamOptions getStreamOptions() {
        return streamOptions;
    }

    public void setStreamOptions(StreamOptions streamOptions) {
        this.streamOptions = streamOptions;
    }

    public Thinking getThinking() {
        return thinking;
    }

    public void setThinking(Thinking thinking) {
        this.thinking = thinking;
    }

    public record Message(String role, Object content) {
    }

    public record ContentPart(String type, String text, ImageUrl imageUrl) {
    }

    public record ImageUrl(String url) {
    }

    public record StreamOptions(boolean includeUsage) {
    }

    public record Thinking(String type, String reasoningEffort) {
    }
}
