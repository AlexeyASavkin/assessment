package com.assessment.ai.domain;

/**
 * Результат генерации уточняющего вопроса LLM.
 *
 * <p>Неизменяемый доменный объект, содержащий текст уточняющего вопроса,
 * который задаётся сотруднику после слабого ответа (оценка ≤ 2).
 */
public final class FollowUpResult {

    private final String questionText;

    private FollowUpResult(String questionText) {
        this.questionText = questionText;
    }

    public static FollowUpResult of(String questionText) {
        return new FollowUpResult(questionText);
    }

    public String getQuestionText() {
        return questionText;
    }
}
