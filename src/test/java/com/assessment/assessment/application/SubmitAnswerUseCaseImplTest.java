package com.assessment.assessment.application;

import com.assessment.ai.domain.FollowUpResult;
import com.assessment.ai.domain.ScoreResult;
import com.assessment.ai.port.LlmFollowUpPort;
import com.assessment.assessment.domain.AssessmentSession;
import com.assessment.assessment.domain.Attempt;
import com.assessment.assessment.domain.SessionStatus;
import com.assessment.assessment.domain.TopicInfo;
import com.assessment.assessment.port.out.AttemptRepositoryPort;
import com.assessment.assessment.port.out.SessionRepositoryPort;
import com.assessment.assessment.port.out.TopicQueryPort;
import com.assessment.common.ForbiddenException;
import com.assessment.config.SessionLlmRateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubmitAnswerUseCase: обработка ответа сотрудника")
class SubmitAnswerUseCaseImplTest {

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID ATTEMPT_ID = UUID.randomUUID();
    private static final UUID PARENT_ID = UUID.randomUUID();
    private static final UUID TOPIC_ID = UUID.randomUUID();
    private static final UUID NEXT_TOPIC_ID = UUID.randomUUID();
    private static final String TRANSCRIPT = "Ответ сотрудника";

    @Mock
    private SessionRepositoryPort sessionRepositoryPort;

    @Mock
    private AttemptRepositoryPort attemptRepositoryPort;

    @Mock
    private TopicQueryPort topicQueryPort;

    @Mock
    private LlmFollowUpPort llmFollowUpPort;

    @Mock
    private AttemptScoringExecutor scoringExecutor;

    @Mock
    private SessionQuestionPicker questionPicker;

    private SubmitAnswerUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new SubmitAnswerUseCaseImpl(sessionRepositoryPort, attemptRepositoryPort, topicQueryPort,
                llmFollowUpPort, scoringExecutor, questionPicker,
                new SessionLlmRateLimiter(RateLimiterRegistry.of(RateLimiterConfig.custom()
                        .limitForPeriod(1000)
                        .limitRefreshPeriod(Duration.ofMinutes(1))
                        .timeoutDuration(Duration.ofSeconds(1))
                        .build())));
    }

    @Test
    @DisplayName("Ответ на уточняющий вопрос переоценивает основную попытку с сохранением baseScore")
    void followUpAnswerRescoredAndParentUpdatedWithBaseScorePreserved() {
        Attempt current = attempt(ATTEMPT_ID, TOPIC_ID, 1, PARENT_ID, "Уточняющий вопрос", null, null, null);
        stubSessionAndCurrentAttempt(current);
        when(topicQueryPort.findAll()).thenReturn(List.of());
        Attempt scoredFollowUp = current.withFinalTranscript(TRANSCRIPT)
                .withScore(new BigDecimal("4"), "high", true, "Стало лучше");
        when(scoringExecutor.scoreNow(any())).thenReturn(scoredFollowUp);
        Attempt parent = attempt(PARENT_ID, TOPIC_ID, 0, null, "Основной вопрос", "Слабый ответ", "2", null);
        when(attemptRepositoryPort.findById(PARENT_ID)).thenReturn(Optional.of(parent));
        when(llmFollowUpPort.rescoreMainAttempt("Основной вопрос", "Слабый ответ", "Уточняющий вопрос", TRANSCRIPT))
                .thenReturn(Optional.of(ScoreResult.of(4, "high", "Стало лучше")));
        // у родителя уже есть child (текущая попытка) — новых кандидатов на уточнение нет
        when(attemptRepositoryPort.findBySessionIdOrderByCreatedAtAsc(SESSION_ID))
                .thenReturn(List.of(parent, scoredFollowUp));
        when(questionPicker.findNextTopicId(anyList(), anyList())).thenReturn(null);

        AnswerOutcome outcome = useCase.submitAnswer(SESSION_ID, ATTEMPT_ID, TRANSCRIPT);

        assertInstanceOf(AnswerOutcome.Completed.class, outcome);
        verify(attemptRepositoryPort).save(argThat(a -> PARENT_ID.equals(a.getId())
                && a.getBaseScore() != null && a.getBaseScore().compareTo(new BigDecimal("2")) == 0
                && a.getScore() != null && a.getScore().compareTo(new BigDecimal("4")) == 0
                && "Стало лучше".equals(a.getFeedback())));
        verify(sessionRepositoryPort).save(argThat(s -> s.getStatus() == SessionStatus.COMPLETED));
    }

    @Test
    @DisplayName("При пустом результате переоценки родитель не сохраняется, а для нового кандидата создаётся уточнение")
    void followUpAnswerWithFailedRescoreSkipsParentAndGeneratesNextFollowUp() {
        Attempt current = attempt(ATTEMPT_ID, TOPIC_ID, 1, PARENT_ID, "Уточняющий вопрос", null, null, null);
        stubSessionAndCurrentAttempt(current);
        when(topicQueryPort.findAll()).thenReturn(List.of());
        Attempt scoredFollowUp = current.withFinalTranscript(TRANSCRIPT)
                .withScore(new BigDecimal("3"), "mid", true, "Неплохо");
        when(scoringExecutor.scoreNow(any())).thenReturn(scoredFollowUp);
        Attempt parent = attempt(PARENT_ID, TOPIC_ID, 0, null, "Основной вопрос", "Слабый ответ", "2", null);
        when(attemptRepositoryPort.findById(PARENT_ID)).thenReturn(Optional.of(parent));
        when(llmFollowUpPort.rescoreMainAttempt(any(), any(), any(), any())).thenReturn(Optional.empty());
        UUID weakId = UUID.randomUUID();
        Attempt weakOther = attempt(weakId, TOPIC_ID, 0, null, "Другой вопрос", "Другой слабый ответ", "1", null);
        when(attemptRepositoryPort.findBySessionIdOrderByCreatedAtAsc(SESSION_ID))
                .thenReturn(List.of(parent, scoredFollowUp, weakOther));
        when(llmFollowUpPort.generateFollowUpQuestion("Другой вопрос", "Другой слабый ответ"))
                .thenReturn(Optional.of(FollowUpResult.of("Новый уточняющий вопрос")));

        AnswerOutcome outcome = useCase.submitAnswer(SESSION_ID, ATTEMPT_ID, TRANSCRIPT);

        AnswerOutcome.NextQuestion next = assertInstanceOf(AnswerOutcome.NextQuestion.class, outcome);
        assertEquals(TOPIC_ID, next.topicId());
        assertEquals("Новый уточняющий вопрос", next.attempt().getQuestionText());
        // COR-3: глубина уточнения считается от кандидата (основная попытка depth=0) — всегда 1
        assertEquals(Integer.valueOf(1), next.attempt().getFollowupDepth());
        assertEquals(weakId, next.attempt().getFollowupParentId());
        // переоценка не удалась — родитель не сохраняется
        verify(attemptRepositoryPort, never()).save(argThat(a -> PARENT_ID.equals(a.getId())));
        verify(sessionRepositoryPort).save(any());
    }

    @Test
    @DisplayName("Ответ на уточнение без родителя пропускает переоценку и переходит к следующей теме")
    void followUpAnswerWithoutParentSkipsRescoreAndAdvances() {
        Attempt current = attempt(ATTEMPT_ID, TOPIC_ID, 1, null, "Уточняющий вопрос", null, null, null);
        stubSessionAndCurrentAttempt(current);
        when(topicQueryPort.findAll()).thenReturn(List.of());
        Attempt scoredFollowUp = current.withFinalTranscript(TRANSCRIPT)
                .withScore(new BigDecimal("4"), "high", true, "Хорошо");
        when(scoringExecutor.scoreNow(any())).thenReturn(scoredFollowUp);
        when(attemptRepositoryPort.findBySessionIdOrderByCreatedAtAsc(SESSION_ID))
                .thenReturn(List.of(scoredFollowUp));
        when(questionPicker.findNextTopicId(anyList(), anyList())).thenReturn(NEXT_TOPIC_ID);
        when(questionPicker.pickQuestion(eq(NEXT_TOPIC_ID), anyList())).thenReturn("Вопрос следующей темы");

        AnswerOutcome outcome = useCase.submitAnswer(SESSION_ID, ATTEMPT_ID, TRANSCRIPT);

        AnswerOutcome.NextQuestion next = assertInstanceOf(AnswerOutcome.NextQuestion.class, outcome);
        assertEquals(NEXT_TOPIC_ID, next.topicId());
        assertEquals(Integer.valueOf(0), next.attempt().getFollowupDepth());
        assertEquals("Вопрос следующей темы", next.attempt().getQuestionText());
        verify(llmFollowUpPort, never()).rescoreMainAttempt(any(), any(), any(), any());
        verify(llmFollowUpPort, never()).generateFollowUpQuestion(any(), any());
    }

    @Test
    @DisplayName("Основной ответ при наличии вопросов в теме оценивается синхронно и возвращает следующий вопрос")
    void mainAnswerWithMoreQuestionsInTopicScoresAsyncAndReturnsNext() {
        Attempt current = attempt(ATTEMPT_ID, TOPIC_ID, 0, null, "Основной вопрос", null, null, null);
        stubSessionAndCurrentAttempt(current);
        when(topicQueryPort.findAll()).thenReturn(List.of(topicInfo(TOPIC_ID)));
        when(attemptRepositoryPort.findBySessionIdOrderByCreatedAtAsc(SESSION_ID)).thenReturn(List.of(current));
        when(questionPicker.hasUnused(eq(TOPIC_ID), anyList())).thenReturn(true);
        when(questionPicker.pickQuestion(eq(TOPIC_ID), anyList())).thenReturn("Следующий вопрос темы");

        AnswerOutcome outcome = useCase.submitAnswer(SESSION_ID, ATTEMPT_ID, TRANSCRIPT);

        AnswerOutcome.NextQuestion next = assertInstanceOf(AnswerOutcome.NextQuestion.class, outcome);
        assertEquals(TOPIC_ID, next.topicId());
        assertEquals("Следующий вопрос темы", next.attempt().getQuestionText());
        assertEquals(Integer.valueOf(0), next.attempt().getFollowupDepth());
        verify(scoringExecutor).scoreNow(any());
        verify(scoringExecutor, never()).scoreUnscored(any());
        verify(sessionRepositoryPort).save(any());
    }

    @Test
    @DisplayName("Основной ответ с исчерпанной темой и слабой оценкой приводит к уточняющему вопросу глубины 1")
    void mainAnswerFinishingTopicGeneratesFollowUpForWeakAnswer() {
        Attempt current = attempt(ATTEMPT_ID, TOPIC_ID, 0, null, "Основной вопрос", null, null, null);
        stubSessionAndCurrentAttempt(current);
        when(topicQueryPort.findAll()).thenReturn(List.of(topicInfo(TOPIC_ID)));
        Attempt scoredCurrent = current.withFinalTranscript(TRANSCRIPT)
                .withScore(new BigDecimal("2"), "low", true, "Слабо");
        when(attemptRepositoryPort.findBySessionIdOrderByCreatedAtAsc(SESSION_ID))
                .thenReturn(List.of(scoredCurrent));
        when(questionPicker.hasUnused(eq(TOPIC_ID), anyList())).thenReturn(false);
        when(llmFollowUpPort.generateFollowUpQuestion("Основной вопрос", TRANSCRIPT))
                .thenReturn(Optional.of(FollowUpResult.of("Уточняющий вопрос")));

        AnswerOutcome outcome = useCase.submitAnswer(SESSION_ID, ATTEMPT_ID, TRANSCRIPT);

        AnswerOutcome.NextQuestion next = assertInstanceOf(AnswerOutcome.NextQuestion.class, outcome);
        assertEquals(TOPIC_ID, next.topicId());
        assertEquals("Уточняющий вопрос", next.attempt().getQuestionText());
        assertEquals(Integer.valueOf(1), next.attempt().getFollowupDepth());
        assertEquals(ATTEMPT_ID, next.attempt().getFollowupParentId());
        verify(scoringExecutor).scoreUnscored(SESSION_ID);
        verify(scoringExecutor, never()).scoreNow(any());
    }

    @Test
    @DisplayName("Основной ответ на последней теме без кандидатов завершает сессию статусом COMPLETED")
    void mainAnswerOnLastTopicCompletesSession() {
        Attempt current = attempt(ATTEMPT_ID, TOPIC_ID, 0, null, "Основной вопрос", null, null, null);
        stubSessionAndCurrentAttempt(current);
        when(topicQueryPort.findAll()).thenReturn(List.of(topicInfo(TOPIC_ID)));
        Attempt scoredCurrent = current.withFinalTranscript(TRANSCRIPT)
                .withScore(new BigDecimal("4"), "high", true, "Хорошо");
        when(attemptRepositoryPort.findBySessionIdOrderByCreatedAtAsc(SESSION_ID))
                .thenReturn(List.of(scoredCurrent));
        when(questionPicker.hasUnused(eq(TOPIC_ID), anyList())).thenReturn(false);
        when(questionPicker.findNextTopicId(anyList(), anyList())).thenReturn(null);

        AnswerOutcome outcome = useCase.submitAnswer(SESSION_ID, ATTEMPT_ID, TRANSCRIPT);

        assertInstanceOf(AnswerOutcome.Completed.class, outcome);
        verify(sessionRepositoryPort).save(argThat(s -> s.getStatus() == SessionStatus.COMPLETED));
        verify(scoringExecutor, times(2)).scoreUnscored(SESSION_ID);
        verify(llmFollowUpPort, never()).generateFollowUpQuestion(any(), any());
    }

    @Test
    @DisplayName("Основной ответ без кандидатов на уточнение переводит сессию на следующую тему")
    void mainAnswerWithoutFollowUpCandidateAdvancesToNextTopic() {
        Attempt current = attempt(ATTEMPT_ID, TOPIC_ID, 0, null, "Основной вопрос", null, null, null);
        stubSessionAndCurrentAttempt(current);
        when(topicQueryPort.findAll()).thenReturn(List.of(topicInfo(TOPIC_ID), topicInfo(NEXT_TOPIC_ID)));
        Attempt scoredCurrent = current.withFinalTranscript(TRANSCRIPT)
                .withScore(new BigDecimal("5"), "high", true, "Отлично");
        when(attemptRepositoryPort.findBySessionIdOrderByCreatedAtAsc(SESSION_ID))
                .thenReturn(List.of(scoredCurrent));
        when(questionPicker.hasUnused(eq(TOPIC_ID), anyList())).thenReturn(false);
        when(questionPicker.findNextTopicId(anyList(), anyList())).thenReturn(NEXT_TOPIC_ID);
        when(questionPicker.pickQuestion(eq(NEXT_TOPIC_ID), anyList())).thenReturn("Вопрос следующей темы");

        AnswerOutcome outcome = useCase.submitAnswer(SESSION_ID, ATTEMPT_ID, TRANSCRIPT);

        AnswerOutcome.NextQuestion next = assertInstanceOf(AnswerOutcome.NextQuestion.class, outcome);
        assertEquals(NEXT_TOPIC_ID, next.topicId());
        assertEquals(Integer.valueOf(0), next.attempt().getFollowupDepth());
        assertEquals("Вопрос следующей темы", next.attempt().getQuestionText());
        verify(scoringExecutor).scoreUnscored(SESSION_ID);
        verify(sessionRepositoryPort).save(any());
    }

    @Test
    @DisplayName("Отсутствующая сессия приводит к NoSuchElementException")
    void missingSessionThrows() {
        when(sessionRepositoryPort.findById(SESSION_ID)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> useCase.submitAnswer(SESSION_ID, ATTEMPT_ID, TRANSCRIPT));
    }

    @Test
    @DisplayName("Отсутствующая попытка ответа приводит к NoSuchElementException")
    void missingAttemptThrows() {
        stubActiveSession();
        when(attemptRepositoryPort.findById(ATTEMPT_ID)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> useCase.submitAnswer(SESSION_ID, ATTEMPT_ID, TRANSCRIPT));
    }

    @Test
    @DisplayName("Ответ в завершённую сессию отклоняется ForbiddenException")
    void completedSessionRejectsAnswer() {
        AssessmentSession completed = AssessmentSession.of(SESSION_ID, UUID.randomUUID(), "Иванов Иван", null,
                SessionStatus.COMPLETED, null, Instant.now());
        when(sessionRepositoryPort.findById(SESSION_ID)).thenReturn(Optional.of(completed));

        assertThrows(ForbiddenException.class, () -> useCase.submitAnswer(SESSION_ID, ATTEMPT_ID, TRANSCRIPT));
        verify(attemptRepositoryPort, never()).findById(any());
    }

    @Test
    @DisplayName("Попытка из другой сессии отклоняется ForbiddenException (IDOR-защита)")
    void attemptFromAnotherSessionRejected() {
        stubActiveSession();
        UUID foreignSessionId = UUID.randomUUID();
        Attempt foreign = Attempt.of(ATTEMPT_ID, foreignSessionId, "Чужой вопрос", null, null, null, "high",
                null, null, 0, null, TOPIC_ID, "Тема", "Раздел", "Компетенция", Instant.now());
        when(attemptRepositoryPort.findById(ATTEMPT_ID)).thenReturn(Optional.of(foreign));

        assertThrows(ForbiddenException.class, () -> useCase.submitAnswer(SESSION_ID, ATTEMPT_ID, TRANSCRIPT));
        verify(attemptRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Сбой оценки основного ответа пробрасывается вызывающему")
    void scoringFailurePropagates() {
        Attempt current = attempt(ATTEMPT_ID, TOPIC_ID, 0, null, "Основной вопрос", null, null, null);
        stubSessionAndCurrentAttempt(current);
        when(topicQueryPort.findAll()).thenReturn(List.of(topicInfo(TOPIC_ID)));
        when(attemptRepositoryPort.findBySessionIdOrderByCreatedAtAsc(SESSION_ID)).thenReturn(List.of(current));
        when(questionPicker.hasUnused(eq(TOPIC_ID), anyList())).thenReturn(true);
        when(scoringExecutor.scoreNow(any())).thenThrow(new RuntimeException("LLM недоступен"));

        assertThrows(RuntimeException.class, () -> useCase.submitAnswer(SESSION_ID, ATTEMPT_ID, TRANSCRIPT));
    }

    @Test
    @DisplayName("Пустой результат генерации уточнения не создаёт follow-up и переводит на следующую тему")
    void emptyFollowUpResultSkipsFollowUpAndAdvances() {
        Attempt current = attempt(ATTEMPT_ID, TOPIC_ID, 0, null, "Основной вопрос", null, null, null);
        stubSessionAndCurrentAttempt(current);
        when(topicQueryPort.findAll()).thenReturn(List.of(topicInfo(TOPIC_ID), topicInfo(NEXT_TOPIC_ID)));
        Attempt scoredCurrent = current.withFinalTranscript(TRANSCRIPT)
                .withScore(new BigDecimal("2"), "low", true, "Слабо");
        when(attemptRepositoryPort.findBySessionIdOrderByCreatedAtAsc(SESSION_ID))
                .thenReturn(List.of(scoredCurrent));
        when(questionPicker.hasUnused(eq(TOPIC_ID), anyList())).thenReturn(false);
        when(llmFollowUpPort.generateFollowUpQuestion(any(), any())).thenReturn(Optional.empty());
        when(questionPicker.findNextTopicId(anyList(), anyList())).thenReturn(NEXT_TOPIC_ID);
        when(questionPicker.pickQuestion(eq(NEXT_TOPIC_ID), anyList())).thenReturn("Вопрос следующей темы");

        AnswerOutcome outcome = useCase.submitAnswer(SESSION_ID, ATTEMPT_ID, TRANSCRIPT);

        AnswerOutcome.NextQuestion next = assertInstanceOf(AnswerOutcome.NextQuestion.class, outcome);
        assertEquals(NEXT_TOPIC_ID, next.topicId());
        assertEquals("Вопрос следующей темы", next.attempt().getQuestionText());
        verify(llmFollowUpPort).generateFollowUpQuestion(any(), any());
        verify(attemptRepositoryPort, never()).save(argThat(a -> a.getFollowupDepth() != null && a.getFollowupDepth() > 0));
    }

    @Test
    @DisplayName("Пустой текст уточняющего вопроса от LLM игнорируется, сессия переходит дальше")
    void blankFollowUpQuestionSkippedAndAdvances() {
        Attempt current = attempt(ATTEMPT_ID, TOPIC_ID, 0, null, "Основной вопрос", null, null, null);
        stubSessionAndCurrentAttempt(current);
        when(topicQueryPort.findAll()).thenReturn(List.of(topicInfo(TOPIC_ID), topicInfo(NEXT_TOPIC_ID)));
        Attempt scoredCurrent = current.withFinalTranscript(TRANSCRIPT)
                .withScore(new BigDecimal("1"), "low", true, "Слабо");
        when(attemptRepositoryPort.findBySessionIdOrderByCreatedAtAsc(SESSION_ID))
                .thenReturn(List.of(scoredCurrent));
        when(questionPicker.hasUnused(eq(TOPIC_ID), anyList())).thenReturn(false);
        when(llmFollowUpPort.generateFollowUpQuestion(any(), any()))
                .thenReturn(Optional.of(FollowUpResult.of("   ")));
        when(questionPicker.findNextTopicId(anyList(), anyList())).thenReturn(NEXT_TOPIC_ID);
        when(questionPicker.pickQuestion(eq(NEXT_TOPIC_ID), anyList())).thenReturn("Вопрос следующей темы");

        AnswerOutcome outcome = useCase.submitAnswer(SESSION_ID, ATTEMPT_ID, TRANSCRIPT);

        AnswerOutcome.NextQuestion next = assertInstanceOf(AnswerOutcome.NextQuestion.class, outcome);
        assertEquals(NEXT_TOPIC_ID, next.topicId());
        verify(llmFollowUpPort).generateFollowUpQuestion(any(), any());
        verify(attemptRepositoryPort, never()).save(argThat(a -> a.getFollowupDepth() != null && a.getFollowupDepth() > 0));
    }

    @Test
    @DisplayName("Удалённый родитель уточнения пропускает переоценку без ошибки")
    void followUpAnswerWithMissingParentSkipsRescore() {
        Attempt current = attempt(ATTEMPT_ID, TOPIC_ID, 1, PARENT_ID, "Уточняющий вопрос", null, null, null);
        stubSessionAndCurrentAttempt(current);
        when(topicQueryPort.findAll()).thenReturn(List.of());
        Attempt scoredFollowUp = current.withFinalTranscript(TRANSCRIPT)
                .withScore(new BigDecimal("4"), "high", true, "Хорошо");
        when(scoringExecutor.scoreNow(any())).thenReturn(scoredFollowUp);
        when(attemptRepositoryPort.findById(PARENT_ID)).thenReturn(Optional.empty());
        when(attemptRepositoryPort.findBySessionIdOrderByCreatedAtAsc(SESSION_ID))
                .thenReturn(List.of(scoredFollowUp));
        when(questionPicker.findNextTopicId(anyList(), anyList())).thenReturn(null);

        AnswerOutcome outcome = useCase.submitAnswer(SESSION_ID, ATTEMPT_ID, TRANSCRIPT);

        assertInstanceOf(AnswerOutcome.Completed.class, outcome);
        verify(llmFollowUpPort, never()).rescoreMainAttempt(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Из нескольких слабых ответов кандидатом на уточнение становится первый по времени")
    void followUpCandidatePicksFirstWeakAttemptChronologically() {
        Attempt current = attempt(ATTEMPT_ID, TOPIC_ID, 0, null, "Основной вопрос", null, null, null);
        stubSessionAndCurrentAttempt(current);
        when(topicQueryPort.findAll()).thenReturn(List.of(topicInfo(TOPIC_ID)));
        UUID firstWeakId = UUID.randomUUID();
        UUID secondWeakId = UUID.randomUUID();
        Attempt firstWeak = attempt(firstWeakId, TOPIC_ID, 0, null, "Первый слабый", null, "1", null);
        Attempt secondWeak = attempt(secondWeakId, TOPIC_ID, 0, null, "Второй слабый", null, "2", null);
        Attempt scoredCurrent = current.withFinalTranscript(TRANSCRIPT)
                .withScore(new BigDecimal("4"), "high", true, "Хорошо");
        when(attemptRepositoryPort.findBySessionIdOrderByCreatedAtAsc(SESSION_ID))
                .thenReturn(List.of(scoredCurrent, firstWeak, secondWeak));
        when(questionPicker.hasUnused(eq(TOPIC_ID), anyList())).thenReturn(false);
        when(llmFollowUpPort.generateFollowUpQuestion("Первый слабый", null))
                .thenReturn(Optional.of(FollowUpResult.of("Уточняющий вопрос")));

        AnswerOutcome outcome = useCase.submitAnswer(SESSION_ID, ATTEMPT_ID, TRANSCRIPT);

        AnswerOutcome.NextQuestion next = assertInstanceOf(AnswerOutcome.NextQuestion.class, outcome);
        assertEquals(firstWeakId, next.attempt().getFollowupParentId());
        assertEquals(Integer.valueOf(1), next.attempt().getFollowupDepth());
        verify(llmFollowUpPort).generateFollowUpQuestion("Первый слабый", null);
    }

    private void stubActiveSession() {
        AssessmentSession session = AssessmentSession.of(SESSION_ID, UUID.randomUUID(), "Иванов Иван", null,
                SessionStatus.ACTIVE, null, Instant.now());
        when(sessionRepositoryPort.findById(SESSION_ID)).thenReturn(Optional.of(session));
    }

    private void stubSessionAndCurrentAttempt(Attempt current) {
        AssessmentSession session = AssessmentSession.of(SESSION_ID, UUID.randomUUID(), "Иванов Иван", null,
                SessionStatus.ACTIVE, null, Instant.now());
        when(sessionRepositoryPort.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(attemptRepositoryPort.findById(ATTEMPT_ID)).thenReturn(Optional.of(current));
        when(attemptRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static TopicInfo topicInfo(UUID topicId) {
        return TopicInfo.of(topicId, "Тема", "Раздел", null, "Компетенция");
    }

    private static Attempt attempt(UUID id, UUID topicId, int depth, UUID parentId,
                                   String questionText, String transcript, String score, String feedback) {
        return Attempt.of(id, SESSION_ID, questionText, transcript,
                score == null ? null : new BigDecimal(score), null, "high",
                score == null ? null : Boolean.TRUE, feedback,
                depth, parentId, topicId, "Тема", "Раздел", "Компетенция", Instant.now());
    }
}
