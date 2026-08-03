package com.assessment.assessment.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Доменная модель итогового отчёта по сессии оценки.
 *
 * <p>Неизменяемая, не содержит JPA/Spring-зависимостей. Агрегирует отчёт
 * по темам (средний балл, прохождение, уточняющие оценки, feedback) и
 * полный список попыток для отображения flow уточняющих вопросов.
 */
public final class AssessmentResult {

    private final UUID sessionId;
    private final String employeeName;
    private final boolean passed;
    private final String overallRecommendation;
    private final List<TopicReport> topics;
    private final List<Attempt> attempts;

    private AssessmentResult(UUID sessionId, String employeeName, boolean passed,
                             String overallRecommendation, List<TopicReport> topics, List<Attempt> attempts) {
        this.sessionId = sessionId;
        this.employeeName = employeeName;
        this.passed = passed;
        this.overallRecommendation = overallRecommendation;
        this.topics = topics;
        this.attempts = attempts;
    }

    public static AssessmentResult of(UUID sessionId, String employeeName, boolean passed,
                                      String overallRecommendation, List<TopicReport> topics, List<Attempt> attempts) {
        return new AssessmentResult(sessionId, employeeName, passed, overallRecommendation, topics, attempts);
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public boolean isPassed() {
        return passed;
    }

    public String getOverallRecommendation() {
        return overallRecommendation;
    }

    public List<TopicReport> getTopics() {
        return topics;
    }

    public List<Attempt> getAttempts() {
        return attempts;
    }

    /**
     * Отчёт по одной теме: средний балл, прохождение, оценки уточняющих вопросов и feedback.
     */
    public static final class TopicReport {

        private final UUID topicId;
        private final String topicName;
        private final String sectionName;
        private final String competencyName;
        private final BigDecimal averageScore;
        private final boolean passed;
        private final List<BigDecimal> followUpScores;
        private final List<String> feedbacks;

        private TopicReport(UUID topicId, String topicName, String sectionName, String competencyName,
                            BigDecimal averageScore, boolean passed, List<BigDecimal> followUpScores,
                            List<String> feedbacks) {
            this.topicId = topicId;
            this.topicName = topicName;
            this.sectionName = sectionName;
            this.competencyName = competencyName;
            this.averageScore = averageScore;
            this.passed = passed;
            this.followUpScores = followUpScores;
            this.feedbacks = feedbacks;
        }

        public static TopicReport of(UUID topicId, String topicName, String sectionName, String competencyName,
                                     BigDecimal averageScore, boolean passed, List<BigDecimal> followUpScores,
                                     List<String> feedbacks) {
            return new TopicReport(topicId, topicName, sectionName, competencyName, averageScore, passed,
                    followUpScores, feedbacks);
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

        public BigDecimal getAverageScore() {
            return averageScore;
        }

        public boolean isPassed() {
            return passed;
        }

        public List<BigDecimal> getFollowUpScores() {
            return followUpScores;
        }

        public List<String> getFeedbacks() {
            return feedbacks;
        }
    }
}
