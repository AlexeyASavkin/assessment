package com.assessment.ai.domain;

/**
 * Результат генерации основного вопроса LLM.
 *
 * <p>Неизменяемый доменный объект, содержащий текст сгенерированного вопроса.
 */
public final class QuestionResult {

    private final String questionText;

    private QuestionResult(String questionText) {
        this.questionText = questionText;
    }

    public static QuestionResult of(String questionText) {
        return new QuestionResult(questionText);
    }

    public String getQuestionText() {
        return questionText;
    }
}
