package com.assessment.assessment.application;

import com.assessment.assessment.domain.Attempt;
import com.assessment.assessment.domain.QuestionBankQuestion;
import com.assessment.assessment.domain.TopicInfo;
import com.assessment.assessment.port.out.QuestionBankRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionQuestionPicker: выбор вопросов и тем для сессии")
class SessionQuestionPickerTest {

    private static final UUID TOPIC_ID = UUID.randomUUID();
    private static final UUID OTHER_TOPIC_ID = UUID.randomUUID();

    @Mock
    private QuestionBankRepositoryPort questionBankRepositoryPort;

    private SessionQuestionPicker picker;

    @BeforeEach
    void setUp() {
        picker = new SessionQuestionPicker(questionBankRepositoryPort, 20);
    }

    @Test
    @DisplayName("pickQuestion возвращает первый вопрос банка, если попыток ещё не было")
    void pickQuestionReturnsFirstBankQuestion() {
        when(questionBankRepositoryPort.findByTopicIdOrderBySortOrderAsc(TOPIC_ID))
                .thenReturn(List.of(bankQuestion("Вопрос 1"), bankQuestion("Вопрос 2")));

        assertEquals("Вопрос 1", picker.pickQuestion(TOPIC_ID, List.of()));
    }

    @Test
    @DisplayName("pickQuestion пропускает уже использованные в теме вопросы")
    void pickQuestionSkipsUsedQuestions() {
        when(questionBankRepositoryPort.findByTopicIdOrderBySortOrderAsc(TOPIC_ID))
                .thenReturn(List.of(bankQuestion("Вопрос 1"), bankQuestion("Вопрос 2")));
        List<Attempt> attempts = List.of(attempt(TOPIC_ID, "Вопрос 1", "Ответ"));

        assertEquals("Вопрос 2", picker.pickQuestion(TOPIC_ID, attempts));
    }

    @Test
    @DisplayName("pickQuestion не считает использованными вопросы из других тем")
    void pickQuestionIgnoresAttemptsOfOtherTopics() {
        when(questionBankRepositoryPort.findByTopicIdOrderBySortOrderAsc(TOPIC_ID))
                .thenReturn(List.of(bankQuestion("Вопрос 1")));
        List<Attempt> attempts = List.of(attempt(OTHER_TOPIC_ID, "Вопрос 1", "Ответ"));

        assertEquals("Вопрос 1", picker.pickQuestion(TOPIC_ID, attempts));
    }

    @Test
    @DisplayName("pickQuestion для пустого банка вопросов выбрасывает IllegalStateException")
    void pickQuestionWithEmptyBankThrows() {
        when(questionBankRepositoryPort.findByTopicIdOrderBySortOrderAsc(TOPIC_ID)).thenReturn(List.of());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> picker.pickQuestion(TOPIC_ID, List.of()));
        assertTrue(exception.getMessage().contains("Для темы нет сгенерированных вопросов"));
    }

    @Test
    @DisplayName("pickQuestion, когда все вопросы использованы, выбрасывает IllegalStateException")
    void pickQuestionWithAllQuestionsUsedThrows() {
        when(questionBankRepositoryPort.findByTopicIdOrderBySortOrderAsc(TOPIC_ID))
                .thenReturn(List.of(bankQuestion("Вопрос 1")));
        List<Attempt> attempts = List.of(attempt(TOPIC_ID, "Вопрос 1", "Ответ"));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> picker.pickQuestion(TOPIC_ID, attempts));
        assertTrue(exception.getMessage().contains("Все вопросы из банка для темы уже использованы"));
    }

    @Test
    @DisplayName("hasUnused возвращает true, если в банке остался неиспользованный вопрос")
    void hasUnusedReturnsTrueWhenQuestionNotUsed() {
        when(questionBankRepositoryPort.findByTopicIdOrderBySortOrderAsc(TOPIC_ID))
                .thenReturn(List.of(bankQuestion("Вопрос 1"), bankQuestion("Вопрос 2")));
        List<Attempt> attempts = List.of(attempt(TOPIC_ID, "Вопрос 1", "Ответ"));

        assertTrue(picker.hasUnused(TOPIC_ID, attempts));
    }

    @Test
    @DisplayName("hasUnused возвращает false, если все вопросы банка использованы")
    void hasUnusedReturnsFalseWhenAllQuestionsUsed() {
        when(questionBankRepositoryPort.findByTopicIdOrderBySortOrderAsc(TOPIC_ID))
                .thenReturn(List.of(bankQuestion("Вопрос 1")));
        List<Attempt> attempts = List.of(attempt(TOPIC_ID, "Вопрос 1", "Ответ"));

        assertFalse(picker.hasUnused(TOPIC_ID, attempts));
    }

    @Test
    @DisplayName("findNextTopicId возвращает первую тему без отвеченных попыток")
    void findNextTopicIdReturnsFirstUnansweredTopic() {
        UUID secondTopicId = UUID.randomUUID();
        UUID thirdTopicId = UUID.randomUUID();
        List<TopicInfo> topics = List.of(
                topicInfo(TOPIC_ID), topicInfo(secondTopicId), topicInfo(thirdTopicId));
        List<Attempt> attempts = List.of(attempt(TOPIC_ID, "Вопрос 1", "Ответ"));

        assertEquals(secondTopicId, picker.findNextTopicId(attempts, topics));
    }

    @Test
    @DisplayName("findNextTopicId игнорирует попытки с пустым транскриптом и без темы")
    void findNextTopicIdIgnoresBlankTranscriptsAndNullTopics() {
        List<TopicInfo> topics = List.of(topicInfo(TOPIC_ID));
        List<Attempt> attempts = List.of(
                attempt(TOPIC_ID, "Вопрос 1", "   "),
                attempt(null, "Вопрос 2", "Ответ"));

        assertEquals(TOPIC_ID, picker.findNextTopicId(attempts, topics));
    }

    @Test
    @DisplayName("findNextTopicId возвращает null, когда все темы отвечены")
    void findNextTopicIdReturnsNullWhenAllTopicsAnswered() {
        List<TopicInfo> topics = List.of(topicInfo(TOPIC_ID), topicInfo(OTHER_TOPIC_ID));
        List<Attempt> attempts = List.of(
                attempt(TOPIC_ID, "Вопрос 1", "Ответ"),
                attempt(OTHER_TOPIC_ID, "Вопрос 2", "Ответ"));

        assertNull(picker.findNextTopicId(attempts, topics));
    }

    @Test
    @DisplayName("hasReachedQuestionLimit возвращает false, пока лимит не достигнут")
    void hasReachedQuestionLimitReturnsFalseBelowLimit() {
        List<Attempt> attempts = List.of(attempt(TOPIC_ID, "Вопрос 1", "Ответ"));

        assertFalse(picker.hasReachedQuestionLimit(attempts));
    }

    @Test
    @DisplayName("hasReachedQuestionLimit возвращает true при достижении лимита сессии")
    void hasReachedQuestionLimitReturnsTrueAtLimit() {
        List<Attempt> attempts = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            attempts.add(attempt(TOPIC_ID, "Вопрос " + i, "Ответ"));
        }

        assertTrue(picker.hasReachedQuestionLimit(attempts));
    }

    @Test
    @DisplayName("hasReachedQuestionLimit учитывает и уточняющие вопросы")
    void hasReachedQuestionLimitCountsFollowUpAttempts() {
        List<Attempt> attempts = new ArrayList<>();
        for (int i = 0; i < 19; i++) {
            attempts.add(attempt(TOPIC_ID, "Вопрос " + i, "Ответ"));
        }
        attempts.add(attempt(TOPIC_ID, "Уточнение", "Ответ", 1));

        assertTrue(picker.hasReachedQuestionLimit(attempts));
    }

    private static QuestionBankQuestion bankQuestion(String questionText) {
        return QuestionBankQuestion.of(UUID.randomUUID(), TOPIC_ID, questionText);
    }

    private static TopicInfo topicInfo(UUID topicId) {
        return TopicInfo.of(topicId, "Тема", "Раздел", null, "Компетенция");
    }

    private static Attempt attempt(UUID topicId, String questionText, String transcript) {
        return Attempt.of(UUID.randomUUID(), UUID.randomUUID(), questionText, transcript, null, null,
                null, null, null, 0, null, topicId, null, null, null, Instant.now());
    }

    private static Attempt attempt(UUID topicId, String questionText, String transcript, int followupDepth) {
        return Attempt.of(UUID.randomUUID(), UUID.randomUUID(), questionText, transcript, null, null,
                null, null, null, followupDepth, null, topicId, null, null, null, Instant.now());
    }
}
