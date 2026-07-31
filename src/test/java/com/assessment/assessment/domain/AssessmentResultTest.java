package com.assessment.assessment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AssessmentResult: неизменяемая доменная модель итогового отчёта")
class AssessmentResultTest {

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID TOPIC_ID = UUID.randomUUID();

    @Test
    @DisplayName("of сохраняет все поля, включая вложенные отчёты по темам и попытки")
    void ofStoresAllFields() {
        AssessmentResult.TopicReport topicReport = topicReport();
        Attempt attempt = Attempt.of(UUID.randomUUID(), SESSION_ID, "Вопрос", "Транскрипт",
                new BigDecimal("4"), null, "high", Boolean.TRUE, "Хорошо",
                0, null, TOPIC_ID, "Тема", "Раздел", "Компетенция", Instant.now());
        List<AssessmentResult.TopicReport> topics = List.of(topicReport);
        List<Attempt> attempts = List.of(attempt);

        AssessmentResult result = AssessmentResult.of(SESSION_ID, "Иванов Иван", true,
                "Рекомендуется к зачёту", topics, attempts);

        assertEquals(SESSION_ID, result.getSessionId());
        assertEquals("Иванов Иван", result.getEmployeeName());
        assertTrue(result.isPassed());
        assertEquals("Рекомендуется к зачёту", result.getOverallRecommendation());
        assertSame(topics, result.getTopics());
        assertSame(attempts, result.getAttempts());
        assertSame(topicReport, result.getTopics().get(0));
        assertSame(attempt, result.getAttempts().get(0));
    }

    @Test
    @DisplayName("TopicReport.of сохраняет все поля отчёта по теме")
    void topicReportOfStoresAllFields() {
        AssessmentResult.TopicReport report = topicReport();

        assertEquals(TOPIC_ID, report.getTopicId());
        assertEquals("Stream API", report.getTopicName());
        assertEquals("Java Core", report.getSectionName());
        assertEquals("Java", report.getCompetencyName());
        assertEquals(new BigDecimal("4.50"), report.getAverageScore());
        assertTrue(report.isPassed());
        assertEquals(List.of(new BigDecimal("4")), report.getFollowUpScores());
        assertEquals(List.of("Хорошее понимание"), report.getFeedbacks());
    }

    @Test
    @DisplayName("TopicReport возвращает переданные списки уточняющих оценок и feedback")
    void topicReportKeepsProvidedLists() {
        List<BigDecimal> followUpScores = List.of(new BigDecimal("3"), new BigDecimal("5"));
        List<String> feedbacks = List.of("Первый", "Второй");

        AssessmentResult.TopicReport report = AssessmentResult.TopicReport.of(TOPIC_ID, "Тема", "Раздел",
                "Компетенция", new BigDecimal("4.00"), true, followUpScores, feedbacks);

        assertSame(followUpScores, report.getFollowUpScores());
        assertSame(feedbacks, report.getFeedbacks());
    }

    private static AssessmentResult.TopicReport topicReport() {
        return AssessmentResult.TopicReport.of(TOPIC_ID, "Stream API", "Java Core", "Java",
                new BigDecimal("4.50"), true, List.of(new BigDecimal("4")), List.of("Хорошее понимание"));
    }
}
