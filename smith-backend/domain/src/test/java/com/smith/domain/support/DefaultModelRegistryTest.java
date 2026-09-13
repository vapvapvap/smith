package com.smith.domain.support;

import com.smith.domain.exception.UnknownModelException;
import com.smith.domain.model.LlmModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultModelRegistryTest {

    private final DefaultModelRegistry registry = new DefaultModelRegistry();

    @Test
    void resolvesAliasCaseInsensitively() {
        assertThat(registry.resolve("deepseek_flash")).isEqualTo(LlmModel.DEEPSEEK_FLASH);
        assertThat(registry.resolve("DEEPSEEK_V4_PRO")).isEqualTo(LlmModel.DEEPSEEK_V4_PRO);
    }

    @Test
    void listsAllAvailableModels() {
        assertThat(registry.availableModels())
                .containsExactlyInAnyOrder(LlmModel.values());
    }

    @Test
    void throwsOnUnknownAlias() {
        assertThatThrownBy(() -> registry.resolve("UNKNOWN_MODEL"))
                .isInstanceOf(UnknownModelException.class)
                .hasMessageContaining("UNKNOWN_MODEL")
                .hasMessageContaining("DEEPSEEK_V4_PRO");
    }
}
