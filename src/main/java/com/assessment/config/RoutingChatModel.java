package com.assessment.config;

import com.assessment.service.AiProviderService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.Map;

/**
 * Маршрутизатор запросов к LLM между провайдерами Gemini и GigaChat.
 * <p>
 * Делегирует вызовы активному провайдеру, выбранному через
 * переменную окружения {@code AI_PROVIDER}.
 */
public class RoutingChatModel implements ChatModel {

    private final Map<String, ChatModel> delegates;
    private final AiProviderService aiProviderService;

    /**
     * Конструктор маршрутизатора.
     *
     * @param delegates карта делегатов: ключ — имя провайдера, значение — модель
     * @param aiProviderService сервис, определяющий активного провайдера
     */
    public RoutingChatModel(Map<String, ChatModel> delegates, AiProviderService aiProviderService) {
        this.delegates = delegates;
        this.aiProviderService = aiProviderService;
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
        ChatModel delegate = delegates.get(provider);
        if (delegate == null) {
            throw new IllegalStateException("AI провайдер '" + provider + "' не доступен. Доступные: " + delegates.keySet());
        }
        return delegate.call(prompt);
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