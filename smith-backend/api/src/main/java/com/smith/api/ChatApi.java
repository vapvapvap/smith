package com.smith.api;

import com.smith.api.dto.ChatCompletionRequest;
import com.smith.api.dto.ChatCompletionResponse;
import com.smith.api.dto.ChatModelDto;
import com.smith.api.dto.FactsRequest;
import com.smith.api.dto.FactsResponse;
import com.smith.api.dto.SummarizeRequest;
import com.smith.api.dto.SummarizeResponse;
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

    @Operation(summary = "Саммаризация контекста диалога (JSON)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Текст саммари",
                    content = @Content(schema = @Schema(implementation = SummarizeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос или неизвестная модель"),
            @ApiResponse(responseCode = "502", description = "Ошибка LLM-провайдера")
    })
    SummarizeResponse summarize(SummarizeRequest request);

    @Operation(summary = "Извлечение и обновление фактов диалога (JSON)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Обновлённые факты",
                    content = @Content(schema = @Schema(implementation = FactsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос или неизвестная модель"),
            @ApiResponse(responseCode = "502", description = "Ошибка LLM-провайдера")
    })
    FactsResponse facts(FactsRequest request);

    @Operation(summary = "Список доступных моделей")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список моделей",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ChatModelDto.class))))
    })
    List<ChatModelDto> models();
}
