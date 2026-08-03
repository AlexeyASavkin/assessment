package com.assessment.assessment.application;

import java.util.UUID;

/**
 * Use case отправки ответа сотрудника.
 *
 * <p>Сохраняет транскрипт ответа, при необходимости оценивает его через LLM
 * (синхронно для уточняющих вопросов, асинхронно для основных), генерирует
 * уточняющие вопросы для слабых ответов и возвращает следующий вопрос либо
 * завершает сессию, когда все темы пройдены.
 */
public interface SubmitAnswerUseCase {

    /**
     * Принимает ответ сотрудника и возвращает следующий вопрос или завершение сессии.
     *
     * @param sessionId        идентификатор сессии
     * @param questionAttemptId идентификатор попытки, на которую отвечает сотрудник
     * @param finalTranscript  итоговый текст ответа
     * @return следующий вопрос {@link AnswerOutcome.NextQuestion} или завершение {@link AnswerOutcome.Completed}
     */
    AnswerOutcome submitAnswer(UUID sessionId, UUID questionAttemptId, String finalTranscript);
}
