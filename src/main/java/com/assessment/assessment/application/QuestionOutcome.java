package com.assessment.assessment.application;

import com.assessment.assessment.domain.Attempt;

import java.util.UUID;

/**
 * Результат получения текущего вопроса сессии.
 *
 * <p>Либо готовый вопрос {@link Question}, либо признак завершения {@link Completed}.
 */
public sealed interface QuestionOutcome permits QuestionOutcome.Question, QuestionOutcome.Completed {

    /** Вопрос для отображения сотруднику. */
    record Question(Attempt attempt) implements QuestionOutcome {}

    /** Сессия завершена — вопросов больше нет. */
    record Completed() implements QuestionOutcome {}
}
