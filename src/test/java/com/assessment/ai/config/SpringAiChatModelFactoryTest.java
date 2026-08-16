package com.assessment.ai.config;

import com.assessment.config.StubChatModel;
import com.assessment.service.AiProviderService;
import chat.giga.springai.GigaChatModel;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpringAiChatModelFactory: фабрика делегатов ChatModel")
class SpringAiChatModelFactoryTest {

    @Mock
    private AiProviderService aiProviderService;

    @TempDir
    Path tempDir;

    private SpringAiChatModelFactory factory;

    @BeforeEach
    void setUp() {
        factory = newFactory(false, null);
    }

    private SpringAiChatModelFactory newFactory(boolean unsafeSsl, org.springframework.core.io.Resource caCerts) {
        return new SpringAiChatModelFactory(
                aiProviderService, null, RetryUtils.DEFAULT_RETRY_TEMPLATE, ObservationRegistry.NOOP, unsafeSsl, caCerts);
    }

    // ---- gigachat delegate ----

    @Test
    @DisplayName("create('gigachat') возвращает null без API-ключа")
    void createGigachatWithoutKeyReturnsNull() {
        when(aiProviderService.getApiKey("gigachat")).thenReturn("");

        assertNull(factory.create("gigachat"));
    }

    @Test
    @DisplayName("create('gigachat') строит GigaChatModel с ключом и безопасным SSL по умолчанию")
    void createGigachatWithKeyBuildsModelWithSecureSsl() {
        when(aiProviderService.getApiKey("gigachat")).thenReturn("test-key");

        assertInstanceOf(GigaChatModel.class, factory.create("gigachat"));
    }

    @Test
    @DisplayName("create('gigachat') с unsafeSsl=true строит модель с отключённой проверкой SSL")
    void createGigachatWithUnsafeSslTrueBuildsModel() {
        factory = newFactory(true, null);
        when(aiProviderService.getApiKey("gigachat")).thenReturn("test-key");

        assertNotNull(factory.create("gigachat"));
    }

    @Test
    @DisplayName("create('gigachat') строит TrustManagerFactory из CA-бандла")
    void createGigachatWithCaCertsBuildsTrustManager() {
        factory = newFactory(false, new ClassPathResource("certs/test-ca.pem"));
        when(aiProviderService.getApiKey("gigachat")).thenReturn("test-key");

        assertNotNull(factory.create("gigachat"));
    }

    @Test
    @DisplayName("create('gigachat') падает при нечитаемом CA-бандле")
    void createGigachatWithMissingCaCertsFailsFast() {
        factory = newFactory(false, new FileSystemResource(tempDir.resolve("missing-ca.pem")));
        when(aiProviderService.getApiKey("gigachat")).thenReturn("test-key");

        assertThrows(IllegalStateException.class, () -> factory.create("gigachat"));
    }

    // ---- другие провайдеры ----

    @Test
    @DisplayName("create('stub') возвращает StubChatModel")
    void createStubReturnsStubChatModel() {
        assertInstanceOf(StubChatModel.class, factory.create("stub"));
    }

    @Test
    @DisplayName("create('unknown') возвращает null")
    void createUnknownProviderReturnsNull() {
        assertNull(factory.create("unknown"));
    }

    // ---- availableProviders ----

    @Test
    @DisplayName("availableProviders включает gigachat при наличии ключа")
    void availableProvidersIncludesGigachatWhenKeyPresent() {
        when(aiProviderService.getApiKey("opencode")).thenReturn("");
        when(aiProviderService.getApiKey("gigachat")).thenReturn("test-key");
        when(aiProviderService.getApiKey("openrouter")).thenReturn("");
        when(aiProviderService.getApiKey("gemini")).thenReturn("");

        Set<String> providers = factory.availableProviders();

        assertTrue(providers.contains("stub"));
        assertTrue(providers.contains("gigachat"));
    }

    @Test
    @DisplayName("availableProviders исключает gigachat без ключа")
    void availableProvidersExcludesGigachatWithoutKey() {
        when(aiProviderService.getApiKey("opencode")).thenReturn("");
        when(aiProviderService.getApiKey("gigachat")).thenReturn("");
        when(aiProviderService.getApiKey("openrouter")).thenReturn("");
        when(aiProviderService.getApiKey("gemini")).thenReturn("");

        Set<String> providers = factory.availableProviders();

        assertFalse(providers.contains("gigachat"));
    }
}