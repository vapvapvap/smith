package com.smith.api;

import com.smith.api.dto.ChatCompletionRequest;
import com.smith.api.dto.ChatCompletionResponse;
import com.smith.api.dto.ChatModelDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "chat", description = "Работа с генерацией ответов нейросети")
public interface ChatApi {

    @Operation(summary = "Синхронная генерация ответа (JSON)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ответ нейросети",
                    content = @Content(schema = @Schema(implementation = ChatCompletionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос или неизвестная модель"),
            @ApiResponse(responseCode = "502", description = "Ошибка LLM-провайдера")
    })
    ChatCompletionResponse complete(ChatCompletionRequest request);

    @Operation(summary = "Список доступных моделей")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список моделей",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ChatModelDto.class))))
    })
    List<ChatModelDto> models();
}
