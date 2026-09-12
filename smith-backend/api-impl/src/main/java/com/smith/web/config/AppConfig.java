package com.smith.web.config;

import com.smith.application.service.ChatCompletionService;
import com.smith.application.service.ModelInfoService;
import com.smith.domain.port.ChatRequestRepository;
import com.smith.domain.port.LlmProvider;
import com.smith.domain.port.ModelRegistry;
import com.smith.domain.port.UserRepository;
import com.smith.domain.support.DefaultModelRegistry;
import com.smith.infrastructure.llm.DeepSeekConfig;
import com.smith.infrastructure.llm.DeepSeekLlmProvider;
import com.smith.infrastructure.persistence.ChatRequestMapper;
import com.smith.infrastructure.persistence.PersistenceChatRequestRepository;
import com.smith.infrastructure.persistence.PersistenceUserRepository;
import com.smith.infrastructure.persistence.UserMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableConfigurationProperties(DeepSeekProperties.class)
public class AppConfig {

    @Bean
    public ModelRegistry modelRegistry() {
        return new DefaultModelRegistry();
    }

    @Bean
    public LlmProvider llmProvider(DeepSeekProperties properties) {
        return new DeepSeekLlmProvider(new DeepSeekConfig(
                properties.getBaseUrl(),
                properties.getApiKey(),
                properties.getConnectTimeout(),
                properties.getReadTimeout()));
    }

    @Bean
    public ChatRequestRepository chatRequestRepository(ChatRequestMapper mapper) {
        return new PersistenceChatRequestRepository(mapper);
    }

    @Bean
    public UserRepository userRepository(UserMapper mapper) {
        return new PersistenceUserRepository(mapper);
    }

    @Bean
    public ChatCompletionService chatCompletionService(LlmProvider llmProvider,
                                                       ChatRequestRepository repository,
                                                       ModelRegistry modelRegistry) {
        return new ChatCompletionService(llmProvider, repository, modelRegistry);
    }

    @Bean
    public ModelInfoService modelInfoService(ModelRegistry modelRegistry) {
        return new ModelInfoService(modelRegistry);
    }

    @Bean
    public ExecutorService streamExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
