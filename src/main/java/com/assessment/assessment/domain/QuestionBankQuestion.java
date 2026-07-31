package com.assessment.assessment.domain;

import java.util.UUID;

/**
 * Доменная модель вопроса из банка вопросов.
 *
 * <p>Неизменяемая, не содержит JPA/Spring-зависимостей. Используется
 * use case'ами session flow для выбора вопроса по теме.
 */
public final class QuestionBankQuestion {

    private final UUID id;
    private final UUID topicId;
    private final String questionText;

    private QuestionBankQuestion(UUID id, UUID topicId, String questionText) {
        this.id = id;
        this.topicId = topicId;
        this.questionText = questionText;
    }

    public static QuestionBankQuestion of(UUID id, UUID topicId, String questionText) {
        return new QuestionBankQuestion(id, topicId, questionText);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTopicId() {
        return topicId;
    }

    public String getQuestionText() {
        return questionText;
    }
}
