package com.smith.domain.model;

public record Attachment(String name, String mimeType, String data) {

    public Attachment {
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("Attachment mimeType must not be blank");
        }
        if (data == null || data.isBlank()) {
            throw new IllegalArgumentException("Attachment data must not be blank");
        }
    }

    public boolean isImage() {
        return mimeType.startsWith("image/");
    }

    public String dataUri() {
        return "data:" + mimeType + ";base64," + data;
    }
}
