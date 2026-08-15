package com.assessment.management.application;

import java.util.List;
import java.util.Map;

/**
 * Входной порт (use case) управления настройками ИИ: активным провайдером
 * и промптами (оценка, генерация вопроса, уточняющий вопрос, переоценка).
 *
 * <p>Делегирует чтение/запись настроек сервису провайдеров ИИ
 * (таблица {@code ai_settings}).
 */
public interface AiSettingsUseCase {

    /**
     * Возвращает название активного провайдера LLM.
     *
     * @return название провайдера (opencode, gigachat, openrouter, gemini, stub)
     */
    String getActiveProvider();

    /**
     * Возвращает список доступных для выбора провайдеров.
     *
     * @return список названий провайдеров
     */
    List<String> getAvailableProviders();

    /**
     * Устанавливает активного провайдера LLM.
     *
     * @param provider название провайдера
     * @throws IllegalArgumentException если провайдер неизвестен
     */
    void setActiveProvider(String provider);

    /**
     * Возвращает все промты в виде карты ключ → значение.
     *
     * @return карта промтов (ключи: prompt_scoring, prompt_question, prompt_followup, prompt_rescore)
     */
    Map<String, String> getAllPrompts();

    /**
     * Сохраняет промт по ключу.
     *
     * @param key   ключ промта
     * @param value текст промта
     */
    void setPrompt(String key, String value);
}
