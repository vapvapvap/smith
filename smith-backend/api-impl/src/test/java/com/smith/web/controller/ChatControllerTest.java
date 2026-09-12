package com.smith.web.controller;

import com.smith.api.dto.ChatCompletionRequest;
import com.smith.api.dto.ChatCompletionResponse;
import com.smith.api.dto.ChatModelDto;
import com.smith.application.service.ChatCompletionService;
import com.smith.application.service.ModelInfoService;
import com.smith.domain.exception.UnknownModelException;
import com.smith.domain.model.FinishReason;
import com.smith.domain.model.Usage;
import com.smith.domain.port.ChatStreamListener;
import com.smith.web.error.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatControllerTest {

    private static final String JSON_UTF8 = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8";

    private final ChatCompletionService completionService = mock(ChatCompletionService.class);
    private final ModelInfoService modelInfoService = mock(ModelInfoService.class);
    private final ExecutorService streamExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private MockMvc mvc;

    @BeforeEach
    void setUp() throws Exception {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        ChatController controller = new ChatController(completionService, modelInfoService, streamExecutor);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        streamExecutor.shutdown();
    }

    @Test
    void syncCompletionReturnsJson() throws Exception {
        ChatCompletionResponse response = new ChatCompletionResponse();
        response.setId("resp-1");
        response.setModel("deepseek-v4-flash");
        response.setContent("Привет");
        response.setFinishReason(FinishReason.STOP.name());
        response.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        when(completionService.complete(any(ChatCompletionRequest.class))).thenReturn(response);

        mvc.perform(post("/api/v1/chat/completions")
                        .contentType(JSON_UTF8)
                        .content("{\"prompt\":\"привет\",\"model\":\"DEEPSEEK_V4_FLASH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("resp-1"))
                .andExpect(jsonPath("$.model").value("deepseek-v4-flash"))
                .andExpect(jsonPath("$.content").value("Привет"))
                .andExpect(jsonPath("$.finishReason").value("STOP"));
    }

    @Test
    void unknownModelReturns400() throws Exception {
        when(completionService.complete(any(ChatCompletionRequest.class)))
                .thenThrow(new UnknownModelException("NO_MODEL", List.of("DEEPSEEK_V4_PRO")));

        mvc.perform(post("/api/v1/chat/completions")
                        .contentType(JSON_UTF8)
                        .content("{\"prompt\":\"hi\",\"model\":\"NO_MODEL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown model"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("NO_MODEL")));
    }

    @Test
    void validationErrorReturns400() throws Exception {
        mvc.perform(post("/api/v1/chat/completions")
                        .contentType(JSON_UTF8)
                        .content("{\"prompt\":\"\",\"model\":\"DEEPSEEK_V4_FLASH\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));
    }

    @Test
    void modelsReturnsList() throws Exception {
        when(modelInfoService.availableModels())
                .thenReturn(List.of(new ChatModelDto("DEEPSEEK_V4_FLASH", "deepseek-v4-flash")));

        mvc.perform(get("/api/v1/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].alias").value("DEEPSEEK_V4_FLASH"))
                .andExpect(jsonPath("$[0].providerName").value("deepseek-v4-flash"));
    }

    @Test
    void streamStartsSseAndEmitsEvents() throws Exception {
        doAnswer(invocation -> {
            ChatStreamListener listener = invocation.getArgument(1);
            listener.onChunk("Прив", "");
            listener.onUsage(new Usage(1, 2, 3));
            listener.onComplete(FinishReason.STOP);
            return null;
        }).when(completionService).stream(any(ChatCompletionRequest.class), any());

        MvcResult mvcResult = mvc.perform(post("/api/v1/chat/completions/stream")
                        .contentType(JSON_UTF8)
                        .content("{\"prompt\":\"привет\",\"model\":\"DEEPSEEK_V4_FLASH\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        String body = mvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body)
                .contains("event:chunk")
                .contains("event:usage")
                .contains("event:done")
                .contains("STOP");
    }
}
