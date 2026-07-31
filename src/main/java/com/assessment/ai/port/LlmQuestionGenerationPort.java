package com.assessment.ai.port;

import com.assessment.ai.domain.QuestionResult;

/**
 * Порт генерации основных вопросов через LLM (выходной порт).
 *
 * <p>Используется use case'ами выбора вопросов для генерации вопроса
 * по компетенции и теме. Адаптеры реализуют порт поверх конкретного
 * LLM-провайдера.
 */
public interface LlmQuestionGenerationPort {

    /**
     * Генерирует основной вопрос для оценки темы.
     *
     * @param competencyName название компетенции
     * @param topicName      название темы
     * @return сгенерированный вопрос
     * @throws IllegalStateException если LLM-модель недоступна
     * @throws RuntimeException      при сбое вызова LLM
     */
    QuestionResult generateQuestion(String competencyName, String topicName);
}
