package com.assessment.config;

import org.springframework.ai.chat.model.ChatModel;

import java.util.Set;

/**
 * Фабрика делегатов {@link ChatModel} для LLM-провайдеров.
 * <p>
 * Позволяет создавать модель чата для конкретного провайдера по требованию,
 * а не только на старте приложения. Это даёт «горячее» переключение активного
 * провайдера через {@code ai_settings} без перезапуска: маршрутизатор может
 * создать делегат для любого настроенного провайдера в любой момент.
 */
public interface ChatModelFactory {

    /**
     * Создаёт модель чата для провайдера.
     *
     * @param provider имя провайдера (gemini, gigachat, openrouter, opencode, stub)
     * @return модель чата или {@code null}, если провайдер не настроен (нет API-ключа)
     */
    ChatModel create(String provider);

    /**
     * Возвращает имена провайдеров, для которых можно создать модель.
     *
     * @return набор доступных провайдеров (включая stub, который доступен всегда)
     */
    Set<String> availableProviders();
}
