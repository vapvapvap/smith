package com.smith.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Вложение к запросу (изображение)")
public class AttachmentDto {

    @Schema(description = "Имя файла", example = "photo.png")
    private String name;

    @JsonProperty("mime_type")
    @Schema(description = "MIME-тип изображения", example = "image/png")
    private String mimeType;

    @Schema(description = "Содержимое файла в base64 (без префикса data:)")
    private String data;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
