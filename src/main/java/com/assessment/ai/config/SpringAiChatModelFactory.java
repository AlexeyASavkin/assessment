package com.assessment.ai.config;

import com.assessment.config.ChatModelFactory;
import com.assessment.config.StubChatModel;
import com.assessment.service.AiProviderService;
import com.google.genai.Client;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Component;

import chat.giga.springai.GigaChatModel;
import chat.giga.springai.GigaChatOptions;
import chat.giga.springai.api.GigaChatApiProperties;
import chat.giga.springai.api.GigaChatInternalProperties;
import chat.giga.springai.api.auth.GigaChatApiScope;
import chat.giga.springai.api.auth.GigaChatAuthProperties;
import chat.giga.springai.api.chat.GigaChatApi;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Фабрика делегатов {@link ChatModel} для всех поддерживаемых LLM-провайдеров.
 * <p>
 * Реализует {@link ChatModelFactory}: создаёт модель чата для провайдера
 * по требованию (включая stub, доступный всегда). Используется
 * {@link com.assessment.config.RoutingChatModel} для «горячего» переключения
 * провайдера через {@code ai_settings} без перезапуска приложения.
 */
@Component
public class SpringAiChatModelFactory implements ChatModelFactory {

    private final AiProviderService aiProviderService;
    private final ToolCallingManager toolCallingManager;
    private final RetryTemplate retryTemplate;
    private final ObservationRegistry observationRegistry;

    public SpringAiChatModelFactory(AiProviderService aiProviderService,
                                    ToolCallingManager toolCallingManager,
                                    RetryTemplate retryTemplate,
                                    ObservationRegistry observationRegistry) {
        this.aiProviderService = aiProviderService;
        this.toolCallingManager = toolCallingManager;
        this.retryTemplate = retryTemplate;
        this.observationRegistry = observationRegistry;
    }

    @Override
    public ChatModel create(String provider) {
        return switch (provider) {
            case "gemini" -> geminiDelegate();
            case "gigachat" -> gigachatDelegate();
            case "openrouter" -> openrouterDelegate();
            case "opencode" -> opencodeDelegate();
            case "stub" -> new StubChatModel();
            default -> null;
        };
    }

    @Override
    public Set<String> availableProviders() {
        Set<String> available = new LinkedHashSet<>();
        available.add("stub");
        if (hasKey("opencode")) {
            available.add("opencode");
        }
        if (hasKey("gigachat")) {
            available.add("gigachat");
        }
        if (hasKey("openrouter")) {
            available.add("openrouter");
        }
        if (hasKey("gemini")) {
            available.add("gemini");
        }
        return available;
    }

    private boolean hasKey(String provider) {
        return !aiProviderService.getApiKey(provider).isBlank();
    }

    private ChatModel geminiDelegate() {
        String geminiKey = aiProviderService.getApiKey("gemini");
        if (geminiKey.isBlank()) {
            return null;
        }
        Client genAiClient = Client.builder().apiKey(geminiKey).build();
        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                .model(GoogleGenAiChatModel.ChatModel.GEMINI_2_0_FLASH)
                .build();
        return GoogleGenAiChatModel.builder()
                .genAiClient(genAiClient)
                .options(options)
                .toolCallingManager(toolCallingManager)
                .retryTemplate(retryTemplate)
                .observationRegistry(observationRegistry)
                .build();
    }

    private ChatModel gigachatDelegate() {
        String gigachatKey = aiProviderService.getApiKey("gigachat");
        if (gigachatKey.isBlank()) {
            return null;
        }
        GigaChatAuthProperties auth = GigaChatAuthProperties.builder()
                .bearer(GigaChatAuthProperties.Bearer.builder()
                        .apiKey(gigachatKey)
                        .build())
                .scope(GigaChatApiScope.GIGACHAT_API_PERS)
                .unsafeSsl(true)
                .build();

        GigaChatInternalProperties internal = new GigaChatInternalProperties();
        internal.setConnectTimeout(Duration.ofSeconds(15));

        GigaChatApiProperties props = GigaChatApiProperties.builder()
                .baseUrl(GigaChatApi.DEFAULT_BASE_URL)
                .auth(auth)
                .internal(internal)
                .build();

        GigaChatApi api = new GigaChatApi(props);
        GigaChatOptions options = GigaChatOptions.builder()
                .model(GigaChatApi.ChatModel.GIGA_CHAT_2)
                .temperature(0.3)
                .build();
        return GigaChatModel.builder()
                .gigaChatApi(api)
                .internalProperties(internal)
                .defaultOptions(options)
                .retryTemplate(retryTemplate)
                .observationRegistry(observationRegistry)
                .build();
    }

    private ChatModel openrouterDelegate() {
        String openrouterKey = aiProviderService.getApiKey("openrouter");
        if (openrouterKey.isBlank()) {
            return null;
        }
        OpenAiChatOptions openrouterOptions = OpenAiChatOptions.builder()
                .apiKey(openrouterKey)
                .baseUrl("https://openrouter.ai/api/v1")
                .model("openai/gpt-4o")
                .temperature(0.3)
                .maxTokens(4000)
                .build();
        return OpenAiChatModel.builder()
                .options(openrouterOptions)
                .toolCallingManager(toolCallingManager)
                .observationRegistry(observationRegistry)
                .build();
    }

    private ChatModel opencodeDelegate() {
        String opencodeKey = aiProviderService.getApiKey("opencode");
        if (opencodeKey.isBlank()) {
            return null;
        }
        OpenAiChatOptions opencodeOptions = OpenAiChatOptions.builder()
                .apiKey(opencodeKey)
                .baseUrl("https://opencode.ai/zen/v1")
                .model("deepseek-v4-flash-free")
                .temperature(0.3)
                .maxTokens(4000)
                .build();
        return OpenAiChatModel.builder()
                .options(opencodeOptions)
                .toolCallingManager(toolCallingManager)
                .observationRegistry(observationRegistry)
                .build();
    }
}
