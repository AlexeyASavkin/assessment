package com.assessment.assessment.application;

import java.util.UUID;

/**
 * Use case получения текущего вопроса сессии.
 *
 * <p>Возвращает уже назначенный сессии вопрос, либо создаёт новый из банка
 * вопросов следующей непройденной темы, либо сообщает о завершении сессии.
 */
public interface GetQuestionUseCase {

    /**
     * Возвращает текущий вопрос сессии или создаёт новый.
     *
     * @param sessionId идентификатор сессии
     * @return вопрос {@link QuestionOutcome.Question} или признак завершения {@link QuestionOutcome.Completed}
     */
    QuestionOutcome getCurrentQuestion(UUID sessionId);
}
