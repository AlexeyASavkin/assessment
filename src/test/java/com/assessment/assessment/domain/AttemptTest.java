package com.assessment.assessment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Attempt: неизменяемая доменная модель попытки ответа")
class AttemptTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID PARENT_ID = UUID.randomUUID();
    private static final UUID TOPIC_ID = UUID.randomUUID();
    private static final Instant CREATED_AT = Instant.now();

    @Test
    @DisplayName("of сохраняет все шестнадцать полей и возвращает их через геттеры")
    void ofStoresAllFields() {
        Attempt attempt = Attempt.of(ID, SESSION_ID, "Вопрос", "Транскрипт",
                new BigDecimal("4"), new BigDecimal("2"), "high", Boolean.TRUE, "Хорошо",
                1, PARENT_ID, TOPIC_ID, "Тема", "Раздел", "Компетенция", CREATED_AT);

        assertEquals(ID, attempt.getId());
        assertEquals(SESSION_ID, attempt.getSessionId());
        assertEquals("Вопрос", attempt.getQuestionText());
        assertEquals("Транскрипт", attempt.getFinalTranscript());
        assertEquals(new BigDecimal("4"), attempt.getScore());
        assertEquals(new BigDecimal("2"), attempt.getBaseScore());
        assertEquals("high", attempt.getConfidence());
        assertEquals(Boolean.TRUE, attempt.getValidJudge());
        assertEquals("Хорошо", attempt.getFeedback());
        assertEquals(Integer.valueOf(1), attempt.getFollowupDepth());
        assertEquals(PARENT_ID, attempt.getFollowupParentId());
        assertEquals(TOPIC_ID, attempt.getTopicId());
        assertEquals("Тема", attempt.getTopicName());
        assertEquals("Раздел", attempt.getSectionName());
        assertEquals("Компетенция", attempt.getCompetencyName());
        assertEquals(CREATED_AT, attempt.getCreatedAt());
    }

    @Test
    @DisplayName("withFinalTranscript возвращает новый экземпляр с транскриптом, оригинал не меняется")
    void withFinalTranscriptReturnsNewInstanceAndKeepsOriginal() {
        Attempt original = unansweredAttempt();

        Attempt answered = original.withFinalTranscript("Ответ сотрудника");

        assertNotSame(original, answered);
        assertNull(original.getFinalTranscript());
        assertEquals("Ответ сотрудника", answered.getFinalTranscript());
        assertEquals(original.getId(), answered.getId());
        assertEquals(original.getQuestionText(), answered.getQuestionText());
        assertEquals(original.getTopicId(), answered.getTopicId());
    }

    @Test
    @DisplayName("withScore возвращает новый экземпляр с оценкой, оригинал не меняется")
    void withScoreReturnsNewInstanceAndKeepsOriginal() {
        Attempt original = unansweredAttempt();

        Attempt scored = original.withScore(new BigDecimal("4"), "high", true, "Хороший ответ");

        assertNotSame(original, scored);
        assertNull(original.getScore());
        assertNull(original.getValidJudge());
        assertEquals(new BigDecimal("4"), scored.getScore());
        assertEquals("high", scored.getConfidence());
        assertEquals(Boolean.TRUE, scored.getValidJudge());
        assertEquals("Хороший ответ", scored.getFeedback());
        assertEquals(original.getFinalTranscript(), scored.getFinalTranscript());
    }

    @Test
    @DisplayName("withBaseScore сохраняет исходную оценку в baseScore, оригинал не меняется")
    void withBaseScoreReturnsNewInstanceAndKeepsOriginal() {
        Attempt original = unansweredAttempt()
                .withScore(new BigDecimal("2"), "low", true, "Слабый ответ");

        Attempt rebased = original.withBaseScore(new BigDecimal("2"));

        assertNotSame(original, rebased);
        assertNull(original.getBaseScore());
        assertEquals(new BigDecimal("2"), rebased.getBaseScore());
        assertEquals(new BigDecimal("2"), rebased.getScore());
        assertEquals(original.getId(), rebased.getId());
        assertEquals(original.getFollowupDepth(), rebased.getFollowupDepth());
    }

    private static Attempt unansweredAttempt() {
        return Attempt.of(ID, SESSION_ID, "Вопрос", null, null, null, null, null, null,
                0, null, TOPIC_ID, "Тема", "Раздел", "Компетенция", CREATED_AT);
    }
}
