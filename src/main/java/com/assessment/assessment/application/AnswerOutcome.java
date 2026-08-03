package com.assessment.assessment.application;

import com.assessment.assessment.domain.Attempt;

import java.util.UUID;

/**
 * Результат отправки ответа сотрудником.
 *
 * <p>Либо следующий вопрос {@link NextQuestion} (основной или уточняющий),
 * либо признак завершения {@link Completed}.
 */
public sealed interface AnswerOutcome permits AnswerOutcome.NextQuestion, AnswerOutcome.Completed {

    /**
     * Следующий вопрос сессии.
     *
     * @param attempt попытка следующего вопроса (уже сохранена)
     * @param topicId идентификатор темы, к которой относится следующий вопрос
     */
    record NextQuestion(Attempt attempt, UUID topicId) implements AnswerOutcome {}

    /** Сессия завершена. */
    record Completed() implements AnswerOutcome {}
}
