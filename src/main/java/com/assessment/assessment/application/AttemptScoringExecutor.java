package com.assessment.assessment.application;

import com.assessment.assessment.domain.Attempt;
import com.assessment.assessment.port.out.AttemptRepositoryPort;
import com.assessment.ai.domain.ScoreResult;
import com.assessment.ai.port.LlmScoringPort;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Исполнитель оценки ответов сотрудника через LLM.
 *
 * <p>Инкапсулирует синхронную, асинхронную и пакетную оценку попыток.
 * Зависит только от {@link AttemptRepositoryPort} и {@link LlmScoringPort}.
 * Асинхронная оценка включена на уровне проекта (см. {@code ScoringService}).
 */
@Component
public class AttemptScoringExecutor {

    private final AttemptRepositoryPort attemptRepositoryPort;
    private final LlmScoringPort llmScoringPort;

    public AttemptScoringExecutor(AttemptRepositoryPort attemptRepositoryPort,
                                  LlmScoringPort llmScoringPort) {
        this.attemptRepositoryPort = attemptRepositoryPort;
        this.llmScoringPort = llmScoringPort;
    }

    /**
     * Синхронно оценивает ответ сотрудника через LLM и сохраняет результат.
     *
     * @param attempt попытка с заполненным транскриптом ответа
     * @return обновлённая попытка с оценкой
     * @throws IllegalStateException если LLM-модель недоступна
     * @throws RuntimeException      при сбое вызова LLM
     */
    public Attempt scoreNow(Attempt attempt) {
        ScoreResult result = llmScoringPort.score(attempt.getQuestionText(), attempt.getFinalTranscript());
        return attemptRepositoryPort.save(
                attempt.withScore(BigDecimal.valueOf(result.getScore()), result.getConfidence(),
                        result.isValid(), result.getFeedback()));
    }

    /**
     * Асинхронно оценивает ответ в фоновом потоке, чтобы сотрудник не ждал LLM.
     *
     * <p>Если попытка уже оценена, ничего не делает. При сбое LLM логирует
     * ошибку и не прерывает основной поток.
     *
     * @param attemptId идентификатор попытки (загружается заново из БД в фоновом потоке)
     */
    @Async
    public void scoreAsync(UUID attemptId) {
        Attempt attempt = attemptRepositoryPort.findById(attemptId).orElseThrow();
        if (attempt.getScore() != null) {
            return;
        }
        try {
            scoreNow(attempt);
        } catch (Exception e) {
            System.err.println("Async scoring failed for attempt " + attemptId + ": " + e.getMessage());
        }
    }

    /**
     * Синхронно оценивает все неоценённые попытки сессии с непустым транскриптом.
     *
     * <p>Вызывается при завершении темы/сессии, чтобы гарантировать наличие
     * оценок для отчёта. При сбое оценки отдельной попытки логирует ошибку
     * и продолжает обработку остальных.
     *
     * @param sessionId идентификатор сессии
     */
    public void scoreUnscored(UUID sessionId) {
        for (Attempt attempt : attemptRepositoryPort.findBySessionIdOrderByCreatedAtAsc(sessionId)) {
            if (attempt.getScore() == null && attempt.getFinalTranscript() != null) {
                try {
                    scoreNow(attempt);
                } catch (Exception e) {
                    System.err.println("Batch scoring failed for attempt " + attempt.getId() + ": " + e.getMessage());
                }
            }
        }
    }
}