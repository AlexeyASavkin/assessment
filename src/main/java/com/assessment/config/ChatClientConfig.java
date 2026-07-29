package com.assessment.config;

import com.assessment.service.AiProviderService;
import com.google.genai.Client;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.retry.RetryTemplate;

import chat.giga.springai.GigaChatModel;
import chat.giga.springai.GigaChatOptions;
import chat.giga.springai.api.GigaChatApiProperties;
import chat.giga.springai.api.GigaChatInternalProperties;
import chat.giga.springai.api.auth.GigaChatApiScope;
import chat.giga.springai.api.auth.GigaChatAuthProperties;
import chat.giga.springai.api.chat.GigaChatApi;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Конфигурация Spring AI ChatClient с поддержкой маршрутизации между LLM-провайдерами.
 * <p>
 * Создает делегаты для Gemini, GigaChat и OpenRouter на основе наличия API-ключей,
 * объединяет их в {@link RoutingChatModel} и предоставляет готовый {@link ChatClient}.
 */
@Configuration
public class ChatClientConfig {

    /**
     * Создает маршрутизируемую модель чата, которая переключается между Gemini, GigaChat и OpenRouter.
     * <p>
     * Для каждого провайдера, у которого задан API-ключ, создается отдельная модель
     * с настроенным retry, observation registry и tool calling. Активный провайдер
     * выбирается через переменную окружения {@code AI_PROVIDER}.
     *
     * @param aiProviderService сервис, предоставляющий API-ключи и активный провайдер
     * @param toolCallingManager менеджер вызова инструментов
     * @param retryTemplate шаблон повторных попыток при сбоях
     * @param observationRegistry реестр наблюдений Micrometer
     * @return маршрутизируемая модель {@link ChatModel}
     */
    @Bean
    public ChatModel routingChatModel(
            AiProviderService aiProviderService,
            ToolCallingManager toolCallingManager,
            RetryTemplate retryTemplate,
            ObservationRegistry observationRegistry) {

        Map<String, ChatModel> delegates = new HashMap<>();

        String geminiKey = aiProviderService.getApiKey("gemini");
        if (geminiKey != null && !geminiKey.isBlank()) {
            Client genAiClient = Client.builder().apiKey(geminiKey).build();
            GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                    .model(GoogleGenAiChatModel.ChatModel.GEMINI_2_0_FLASH)
                    .build();
            GoogleGenAiChatModel geminiModel = GoogleGenAiChatModel.builder()
                    .genAiClient(genAiClient)
                    .options(options)
                    .toolCallingManager(toolCallingManager)
                    .retryTemplate(retryTemplate)
                    .observationRegistry(observationRegistry)
                    .build();
            delegates.put("gemini", geminiModel);
        }

        String gigachatKey = aiProviderService.getApiKey("gigachat");
        if (gigachatKey != null && !gigachatKey.isBlank()) {
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
            GigaChatModel gigachatModel = GigaChatModel.builder()
                    .gigaChatApi(api)
                    .internalProperties(internal)
                    .defaultOptions(options)
                    .retryTemplate(retryTemplate)
                    .observationRegistry(observationRegistry)
                    .build();
            delegates.put("gigachat", gigachatModel);
        }

        String openrouterKey = aiProviderService.getApiKey("openrouter");
        if (openrouterKey != null && !openrouterKey.isBlank()) {
            OpenAiChatOptions openrouterOptions = OpenAiChatOptions.builder()
                    .apiKey(openrouterKey)
                    .baseUrl("https://openrouter.ai/api/v1")
                    .model("openai/gpt-4o")
                    .temperature(0.3)
                    .maxTokens(4000)
                    .build();
            OpenAiChatModel openrouterModel = OpenAiChatModel.builder()
                    .options(openrouterOptions)
                    .toolCallingManager(toolCallingManager)
                    .observationRegistry(observationRegistry)
                    .build();
            delegates.put("openrouter", openrouterModel);
        }

        String opencodeKey = aiProviderService.getApiKey("opencode");
        if (opencodeKey != null && !opencodeKey.isBlank()) {
            OpenAiChatOptions opencodeOptions = OpenAiChatOptions.builder()
                    .apiKey(opencodeKey)
                    .baseUrl("https://opencode.ai/zen/v1")
                    .model("deepseek-v4-flash-free")
                    .temperature(0.3)
                    .maxTokens(4000)
                    .build();
            OpenAiChatModel opencodeModel = OpenAiChatModel.builder()
                    .options(opencodeOptions)
                    .toolCallingManager(toolCallingManager)
                    .observationRegistry(observationRegistry)
                    .build();
            delegates.put("opencode", opencodeModel);
        }

        if (delegates.isEmpty() || "stub".equals(aiProviderService.getActiveProvider())) {
            delegates.put("stub", new StubChatModel());
        }

        return new RoutingChatModel(delegates, aiProviderService);
    }

    /**
     * Создает клиент чата на основе маршрутизируемой модели.
     * <p>
     * Оборачивает модель в {@link RateLimitingChatModelDecorator} для ограничения
     * частоты запросов к LLM.
     *
     * @param routingChatModel маршрутизируемая модель чата
     * @param rateLimiterRegistry реестр rate limiter'ов Resilience4j
     * @return настроенный {@link ChatClient}
     */
    @Bean
    public ChatClient chatClient(
            ChatModel routingChatModel,
            RateLimiterRegistry rateLimiterRegistry) {

        ChatModel rateLimitedModel = new RateLimitingChatModelDecorator(
                routingChatModel, rateLimiterRegistry, "geminiApi");

        return ChatClient.builder(rateLimitedModel)
                .build();
    }

    /**
     * Создает модель чата с rate limiting для прямого вызова из сервисов.
     * <p>
     * {@link ChatClient} в Spring AI 2.0 заменяет опции модели на
     * {@code DefaultChatOptions}, что вызывает {@code ClassCastException}
     * в {@code GoogleGenAiChatModel}. Поэтому сервисы используют
     * {@code ChatModel.call(Prompt)} напрямую, минуя {@code ChatClient}.
     *
     * @param routingChatModel маршрутизируемая модель чата
     * @param rateLimiterRegistry реестр rate limiter'ов Resilience4j
     * @return модель чата с rate limiting
     */
    @Bean
    @Primary
    public ChatModel rateLimitedChatModel(
            ChatModel routingChatModel,
            RateLimiterRegistry rateLimiterRegistry) {

        return new RateLimitingChatModelDecorator(
                routingChatModel, rateLimiterRegistry, "geminiApi");
    }
}
