package com.assessment.ai.config;

import com.assessment.config.RoutingChatModel;
import com.assessment.service.AiProviderService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;

import java.time.Duration;

/**
 * Конфигурация Spring AI ChatClient с поддержкой маршрутизации между LLM-провайдерами.
 * <p>
 * Делегаты для Gemini, GigaChat, OpenRouter и OpenCode создаются по требованию
 * через {@link SpringAiChatModelFactory}, объединяются в {@link RoutingChatModel}
 * и предоставляются как {@link ChatModel}. Повторные попытки при сбоях
 * LLM-вызовов настраиваются явным {@link RetryTemplate}. Rate limiting
 * вынесен на уровень вызовов: сессии сотрудников — персональные bucket'ы
 * через {@code SessionLlmRateLimiter}, админская генерация вопросов — общий
 * bucket {@code geminiApi} (см. {@code QuestionSelector}).
 */
@Configuration
public class ChatClientConfig {

    /**
     * Создает шаблон повторных попыток для LLM-вызовов.
     * <p>
     * Экспоненциальный бэк-офф: до 3 повторных попыток (4 вызова суммарно),
     * стартовая задержка 1 с, удвоение с потолком 10 с. Подходит для
     * транзиентных сбоев API (5xx, перегрузка), не перегружая rate limiter.
     *
     * @return настроенный {@link RetryTemplate}
     */
    @Bean
    public RetryTemplate retryTemplate() {
        RetryPolicy retryPolicy = RetryPolicy.builder()
                .maxRetries(3)
                .delay(Duration.ofSeconds(1))
                .multiplier(2.0)
                .maxDelay(Duration.ofSeconds(10))
                .build();
        return new RetryTemplate(retryPolicy);
    }

    /**
     * Создает маршрутизируемую модель чата, которая переключается между
     * Gemini, GigaChat, OpenRouter, OpenCode и stub.
     * <p>
     * Делегаты создаются лениво через {@link SpringAiChatModelFactory} при первом
     * обращении к провайдеру, поэтому переключение активного провайдера через
     * {@code ai_settings} подхватывается в рантайме без перезапуска.
     *
     * @param aiProviderService сервис, предоставляющий API-ключи и активный провайдер
     * @param chatModelFactory  фабрика делегатов {@link ChatModel} по провайдерам
     * @return маршрутизируемая модель {@link ChatModel}
     */
    @Bean
    public ChatModel routingChatModel(AiProviderService aiProviderService,
                                      SpringAiChatModelFactory chatModelFactory) {
        return new RoutingChatModel(aiProviderService, chatModelFactory);
    }

    /**
     * Создает клиент чата на основе маршрутизируемой модели.
     * <p>
     * Rate limiting не оборачивается на уровне модели — он применяется
     * точечно на уровне вызовов (см. классовый javadoc).
     *
     * @param routingChatModel маршрутизируемая модель чата
     * @return настроенный {@link ChatClient}
     */
    @Bean
    public ChatClient chatClient(ChatModel routingChatModel) {
        return ChatClient.builder(routingChatModel)
                .build();
    }

    /**
     * Создает модель чата для прямого вызова из сервисов.
     * <p>
     * {@link ChatClient} в Spring AI 2.0 заменяет опции модели на
     * {@code DefaultChatOptions}, что вызывает {@code ClassCastException}
     * в {@code GoogleGenAiChatModel}. Поэтому сервисы используют
     * {@code ChatModel.call(Prompt)} напрямую, минуя {@code ChatClient}.
     *
     * @param routingChatModel маршрутизируемая модель чата
     * @return модель чата
     */
    @Bean
    @Primary
    public ChatModel primaryChatModel(ChatModel routingChatModel) {
        return routingChatModel;
    }
}
