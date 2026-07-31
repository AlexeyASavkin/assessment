package com.assessment.assessment.application;

import com.assessment.ai.domain.ScoreResult;
import com.assessment.ai.port.LlmScoringPort;
import com.assessment.assessment.domain.Attempt;
import com.assessment.assessment.port.out.AttemptRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttemptScoringExecutor: синхронная, асинхронная и пакетная оценка ответов")
class AttemptScoringExecutorTest {

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID ATTEMPT_ID = UUID.randomUUID();

    @Mock
    private AttemptRepositoryPort attemptRepositoryPort;

    @Mock
    private LlmScoringPort llmScoringPort;

    private AttemptScoringExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new AttemptScoringExecutor(attemptRepositoryPort, llmScoringPort);
    }

    @Test
    @DisplayName("scoreNow оценивает ответ через LLM и сохраняет результат")
    void scoreNowSavesScoredAttempt() {
        Attempt attempt = unanswered(ATTEMPT_ID, "Вопрос", "Транскрипт ответа");
        when(llmScoringPort.score("Вопрос", "Транскрипт ответа"))
                .thenReturn(ScoreResult.of(4, "high", "Хороший ответ"));
        when(attemptRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Attempt scored = executor.scoreNow(attempt);

        assertEquals(0, scored.getScore().compareTo(new BigDecimal("4")));
        assertEquals("high", scored.getConfidence());
        assertTrue(scored.getValidJudge());
        assertEquals("Хороший ответ", scored.getFeedback());
        verify(attemptRepositoryPort).save(any());
    }

    @Test
    @DisplayName("scoreNow с нулевой оценкой помечает попытку невалидной")
    void scoreNowWithZeroScoreMarksAttemptInvalid() {
        Attempt attempt = unanswered(ATTEMPT_ID, "Вопрос", "Бессвязный ответ");
        when(llmScoringPort.score("Вопрос", "Бессвязный ответ"))
                .thenReturn(ScoreResult.of(0, "low", "Не удалось оценить"));
        when(attemptRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Attempt scored = executor.scoreNow(attempt);

        assertEquals(0, scored.getScore().compareTo(BigDecimal.ZERO));
        assertFalse(scored.getValidJudge());
    }

    @Test
    @DisplayName("scoreAsync пропускает уже оценённую попытку без вызова LLM")
    void scoreAsyncSkipsAlreadyScoredAttempt() {
        Attempt scored = unanswered(ATTEMPT_ID, "Вопрос", "Ответ")
                .withScore(new BigDecimal("3"), "mid", true, "Нормально");
        when(attemptRepositoryPort.findById(ATTEMPT_ID)).thenReturn(Optional.of(scored));

        executor.scoreAsync(ATTEMPT_ID);

        verifyNoInteractions(llmScoringPort);
        verify(attemptRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("scoreAsync оценивает неоценённую попытку и сохраняет результат")
    void scoreAsyncScoresUnscoredAttempt() {
        when(attemptRepositoryPort.findById(ATTEMPT_ID))
                .thenReturn(Optional.of(unanswered(ATTEMPT_ID, "Вопрос", "Ответ")));
        when(llmScoringPort.score("Вопрос", "Ответ")).thenReturn(ScoreResult.of(5, "high", "Отлично"));
        when(attemptRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        executor.scoreAsync(ATTEMPT_ID);

        verify(attemptRepositoryPort).save(argThat(a ->
                a.getScore() != null && a.getScore().compareTo(new BigDecimal("5")) == 0));
    }

    @Test
    @DisplayName("scoreAsync при сбое LLM не пробрасывает исключение и не сохраняет попытку")
    void scoreAsyncSwallowsLlmFailure() {
        when(attemptRepositoryPort.findById(ATTEMPT_ID))
                .thenReturn(Optional.of(unanswered(ATTEMPT_ID, "Вопрос", "Ответ")));
        when(llmScoringPort.score("Вопрос", "Ответ")).thenThrow(new RuntimeException("LLM недоступна"));

        assertDoesNotThrow(() -> executor.scoreAsync(ATTEMPT_ID));
        verify(attemptRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("scoreUnscored оценивает только неоценённые попытки с транскриптом")
    void scoreUnscoredScoresOnlyUnscoredAttemptsWithTranscript() {
        Attempt scored = unanswered(UUID.randomUUID(), "Вопрос 1", "Ответ 1")
                .withScore(new BigDecimal("4"), "high", true, "Хорошо");
        Attempt unscored = unanswered(UUID.randomUUID(), "Вопрос 2", "Ответ 2");
        Attempt withoutTranscript = unanswered(UUID.randomUUID(), "Вопрос 3", null);
        when(attemptRepositoryPort.findBySessionIdOrderByCreatedAtAsc(SESSION_ID))
                .thenReturn(List.of(scored, unscored, withoutTranscript));
        when(llmScoringPort.score("Вопрос 2", "Ответ 2")).thenReturn(ScoreResult.of(3, "mid", "Нормально"));
        when(attemptRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        executor.scoreUnscored(SESSION_ID);

        verify(attemptRepositoryPort, times(1)).save(any());
    }

    @Test
    @DisplayName("scoreUnscored продолжает оценку остальных попыток после сбоя одной")
    void scoreUnscoredContinuesAfterSingleFailure() {
        UUID failedId = UUID.randomUUID();
        UUID succeededId = UUID.randomUUID();
        when(attemptRepositoryPort.findBySessionIdOrderByCreatedAtAsc(SESSION_ID))
                .thenReturn(List.of(unanswered(failedId, "Вопрос 1", "Ответ 1"),
                        unanswered(succeededId, "Вопрос 2", "Ответ 2")));
        when(llmScoringPort.score("Вопрос 1", "Ответ 1")).thenThrow(new RuntimeException("LLM недоступна"));
        when(llmScoringPort.score("Вопрос 2", "Ответ 2")).thenReturn(ScoreResult.of(4, "high", "Хорошо"));
        when(attemptRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> executor.scoreUnscored(SESSION_ID));
        verify(attemptRepositoryPort, times(1)).save(argThat(a -> succeededId.equals(a.getId())));
    }

    private static Attempt unanswered(UUID id, String questionText, String transcript) {
        return Attempt.of(id, SESSION_ID, questionText, transcript, null, null, null, null, null,
                0, null, UUID.randomUUID(), null, null, null, Instant.now());
    }
}
