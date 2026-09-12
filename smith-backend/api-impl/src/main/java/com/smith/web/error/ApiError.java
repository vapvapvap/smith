package com.smith.web.error;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Тело ошибки API")
public class ApiError {

    @Schema(description = "HTTP-статус")
    private final int status;

    @Schema(description = "Краткое описание ошибки")
    private final String error;

    @Schema(description = "Подробное сообщение")
    private final String message;

    public ApiError(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }
}
