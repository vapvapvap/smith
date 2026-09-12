package com.smith.infrastructure.llm.dto;

import java.util.List;

public class DeepSeekCompletionResponse {

    private String id;
    private String model;
    private Long created;
    private List<Choice> choices;
    private DeepSeekUsage usage;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    public DeepSeekUsage getUsage() {
        return usage;
    }

    public void setUsage(DeepSeekUsage usage) {
        this.usage = usage;
    }

    public static class Choice {

        private Message message;
        private String finishReason;

        public Message getMessage() {
            return message;
        }

        public void setMessage(Message message) {
            this.message = message;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }
    }

    public static class Message {

        private String content;
        private String reasoningContent;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getReasoningContent() {
            return reasoningContent;
        }

        public void setReasoningContent(String reasoningContent) {
            this.reasoningContent = reasoningContent;
        }
    }
}
