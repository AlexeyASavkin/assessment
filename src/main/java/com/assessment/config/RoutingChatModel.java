package com.assessment.config;

import com.assessment.service.AiProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Маршрутизатор запросов к LLM между провайдерами Gemini, GigaChat,
 * OpenRouter, OpenCode и stub.
 * <p>
 * Делегирует вызовы активному провайдеру, выбранному через
 * {@link AiProviderService} (env {@code AI_PROVIDER} или таблица
 * {@code ai_settings}). Делегаты создаются лениво через {@link ChatModelFactory}
 * при первом обращении к провайдеру и кэшируются — это позволяет переключать
 * активного провайдера в рантайме без перезапуска приложения.
 */
public class RoutingChatModel implements ChatModel {

    private static final Logger logger = LoggerFactory.getLogger(RoutingChatModel.class);

    private final AiProviderService aiProviderService;
    private final ChatModelFactory delegateFactory;
    private final Map<String, ChatModel> delegates = new ConcurrentHashMap<>();

    /**
     * Конструктор маршрутизатора.
     *
     * @param aiProviderService сервис, определяющий активного провайдера
     * @param delegateFactory   фабрика делегатов для провайдеров
     */
    public RoutingChatModel(AiProviderService aiProviderService, ChatModelFactory delegateFactory) {
        this.aiProviderService = aiProviderService;
        this.delegateFactory = delegateFactory;
    }

    /**
     * Отправляет промпт активному LLM-провайдеру.
     *
     * @param prompt объект промпта с сообщениями и параметрами
     * @return ответ модели {@link ChatResponse}
     * @throws IllegalStateException если активный провайдер не настроен
     */
    @Override
    public ChatResponse call(Prompt prompt) {
        String provider = aiProviderService.getActiveProvider();
        logger.debug("Выбран активный AI-провайдер: {}", provider);
        ChatModel delegate = delegates.computeIfAbsent(provider, this::createDelegate);
        return delegate.call(prompt);
    }

    /**
     * Создаёт делегат для провайдера через фабрику.
     *
     * @param provider имя провайдера
     * @return модель чата
     * @throws IllegalStateException если фабрика не может создать модель (нет API-ключа)
     */
    private ChatModel createDelegate(String provider) {
        logger.debug("Создание нового делегата для провайдера: {}", provider);
        ChatModel delegate = delegateFactory.create(provider);
        if (delegate == null) {
            logger.warn("AI-провайдер '{}' недоступен. Доступные: {}", provider, delegateFactory.availableProviders());
            throw new IllegalStateException(
                    "AI провайдер '" + provider + "' не доступен. Доступные: " + delegateFactory.availableProviders());
        }
        return delegate;
    }

    /**
     * Возвращает имя маршрутизируемой модели.
     *
     * @return строковое имя модели
     */
    public String getName() {
        return "RoutingChatModel";
    }
}
