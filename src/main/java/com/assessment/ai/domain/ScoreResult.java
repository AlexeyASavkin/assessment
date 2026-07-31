package com.assessment.ai.domain;

/**
 * Результат оценки ответа сотрудника LLM.
 *
 * <p>Неизменяемый доменный объект: оценка по шкале 0-5, уровень уверенности
 * и текстовая рекомендация. Значение {@code score == 0} означает, что ответ
 * не удалось оценить ({@code validJudge = false}).
 */
public final class ScoreResult {

    private final int score;
    private final String confidence;
    private final String feedback;

    private ScoreResult(int score, String confidence, String feedback) {
        this.score = score;
        this.confidence = confidence;
        this.feedback = feedback;
    }

    public static ScoreResult of(int score, String confidence, String feedback) {
        return new ScoreResult(score, confidence, feedback);
    }

    public int getScore() {
        return score;
    }

    public String getConfidence() {
        return confidence;
    }

    public String getFeedback() {
        return feedback;
    }

    /**
     * @return {@code true}, если оценка валидна (score != 0), иначе {@code false}
     */
    public boolean isValid() {
        return score != 0;
    }
}
