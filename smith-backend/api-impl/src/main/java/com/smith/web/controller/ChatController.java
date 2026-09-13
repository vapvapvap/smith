package com.smith.web.controller;

import com.smith.api.ChatApi;
import com.smith.api.dto.ChatCompletionRequest;
import com.smith.api.dto.ChatCompletionResponse;
import com.smith.api.dto.ChatModelDto;
import com.smith.api.dto.SummarizeRequest;
import com.smith.api.dto.SummarizeResponse;
import com.smith.application.service.ChatCompletionService;
import com.smith.application.service.ModelInfoService;
import com.smith.web.stream.SseStreamListener;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.ExecutorService;

@RestController
@RequestMapping("/api/v1")
public class ChatController implements ChatApi {

    private final ChatCompletionService completionService;
    private final ModelInfoService modelInfoService;
    private final ExecutorService streamExecutor;

    public ChatController(ChatCompletionService completionService,
                          ModelInfoService modelInfoService,
                          ExecutorService streamExecutor) {
        this.completionService = completionService;
        this.modelInfoService = modelInfoService;
        this.streamExecutor = streamExecutor;
    }

    @PostMapping(value = "/chat/completions", produces = MediaType.APPLICATION_JSON_VALUE)
    @Override
    public ChatCompletionResponse complete(@Valid @RequestBody ChatCompletionRequest request) {
        request.setStream(false);
        return completionService.complete(request);
    }

    @PostMapping(value = "/chat/completions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody ChatCompletionRequest request) {
        request.setStream(true);
        SseEmitter emitter = new SseEmitter(0L);
        streamExecutor.execute(() -> completionService.stream(request, new SseStreamListener(emitter)));
        return emitter;
    }

    @PostMapping(value = "/chat/summarize", produces = MediaType.APPLICATION_JSON_VALUE)
    @Override
    public SummarizeResponse summarize(@Valid @RequestBody SummarizeRequest request) {
        return completionService.summarize(request);
    }

    @GetMapping("/models")
    @Override
    public List<ChatModelDto> models() {
        return modelInfoService.availableModels();
    }
}
