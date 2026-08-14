package com.assessment.ai.port;

import com.assessment.ai.domain.ScoreResult;

/**
 * Порт оценки ответа сотрудника через LLM (выходной порт).
 *
 * <p>Используется use case'ами скоринга для получения оценки ответа
 * по шкале 0-5. Адаптеры реализуют порт поверх конкретного LLM-провайдера
 * (Gemini, GigaChat, OpenRouter, OpenCode, stub).
 */
public interface LlmScoringPort {

    /**
     * Оценивает ответ сотрудника на вопрос.
     *
     * @param questionText текст вопроса
     * @param answerText   транскрипт ответа сотрудника
     * @return результат оценки ({@link ScoreResult})
     * @throws IllegalStateException если LLM-модель недоступна
     * @throws RuntimeException      при сбое вызова LLM
     */
    ScoreResult score(String questionText, String answerText);
}
