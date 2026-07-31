package com.assessment.assessment.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Доменная модель попытки ответа на вопрос.
 *
 * <p>Неизменяемая, не содержит JPA/Spring-зависимостей. Покрывает как основные
 * вопросы (followupDepth=0), так и уточняющие (followupDepth&gt;0), включая
 * оценку LLM и ссылку на родительскую попытку.
 */
public final class Attempt {

    private final UUID id;
    private final UUID sessionId;
    private final String questionText;
    private final String finalTranscript;
    private final BigDecimal score;
    private final BigDecimal baseScore;
    private final String confidence;
    private final Boolean validJudge;
    private final String feedback;
    private final Integer followupDepth;
    private final UUID followupParentId;
    private final UUID topicId;
    private final String topicName;
    private final String sectionName;
    private final String competencyName;
    private final Instant createdAt;

    private Attempt(UUID id, UUID sessionId, String questionText, String finalTranscript,
                    BigDecimal score, BigDecimal baseScore, String confidence, Boolean validJudge,
                    String feedback, Integer followupDepth, UUID followupParentId, UUID topicId,
                    String topicName, String sectionName, String competencyName, Instant createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.questionText = questionText;
        this.finalTranscript = finalTranscript;
        this.score = score;
        this.baseScore = baseScore;
        this.confidence = confidence;
        this.validJudge = validJudge;
        this.feedback = feedback;
        this.followupDepth = followupDepth;
        this.followupParentId = followupParentId;
        this.topicId = topicId;
        this.topicName = topicName;
        this.sectionName = sectionName;
        this.competencyName = competencyName;
        this.createdAt = createdAt;
    }

    public static Attempt of(UUID id, UUID sessionId, String questionText, String finalTranscript,
                             BigDecimal score, BigDecimal baseScore, String confidence, Boolean validJudge,
                             String feedback, Integer followupDepth, UUID followupParentId, UUID topicId,
                             String topicName, String sectionName, String competencyName, Instant createdAt) {
        return new Attempt(id, sessionId, questionText, finalTranscript, score, baseScore, confidence, validJudge,
                feedback, followupDepth, followupParentId, topicId, topicName, sectionName, competencyName, createdAt);
    }

    public Attempt withFinalTranscript(String newFinalTranscript) {
        return new Attempt(id, sessionId, questionText, newFinalTranscript, score, baseScore, confidence, validJudge,
                feedback, followupDepth, followupParentId, topicId, topicName, sectionName, competencyName, createdAt);
    }

    public Attempt withScore(BigDecimal newScore, String newConfidence, boolean newValidJudge, String newFeedback) {
        return new Attempt(id, sessionId, questionText, finalTranscript, newScore, baseScore, newConfidence, newValidJudge,
                newFeedback, followupDepth, followupParentId, topicId, topicName, sectionName, competencyName, createdAt);
    }

    public Attempt withBaseScore(BigDecimal newBaseScore) {
        return new Attempt(id, sessionId, questionText, finalTranscript, score, newBaseScore, confidence, validJudge,
                feedback, followupDepth, followupParentId, topicId, topicName, sectionName, competencyName, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getFinalTranscript() {
        return finalTranscript;
    }

    public BigDecimal getScore() {
        return score;
    }

    public BigDecimal getBaseScore() {
        return baseScore;
    }

    public String getConfidence() {
        return confidence;
    }

    public Boolean getValidJudge() {
        return validJudge;
    }

    public String getFeedback() {
        return feedback;
    }

    public Integer getFollowupDepth() {
        return followupDepth;
    }

    public UUID getFollowupParentId() {
        return followupParentId;
    }

    public UUID getTopicId() {
        return topicId;
    }

    public String getTopicName() {
        return topicName;
    }

    public String getSectionName() {
        return sectionName;
    }

    public String getCompetencyName() {
        return competencyName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
