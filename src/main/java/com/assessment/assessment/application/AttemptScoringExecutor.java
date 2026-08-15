package com.assessment.assessment.application;

import com.assessment.assessment.domain.Attempt;
import com.assessment.assessment.port.out.AttemptRepositoryPort;
import com.assessment.ai.domain.ScoreResult;
import com.assessment.ai.port.LlmScoringPort;
import com.assessment.config.SessionLlmRateLimiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Исполнитель оценки ответов сотрудника через LLM.
 *
 * <p>Инкапсулирует синхронную и пакетную оценку попыток. Каждый LLM-вызов
 * ограничен персональным rate limit'ом сессии через {@link SessionLlmRateLimiter}.
 * Зависит только от {@link AttemptRepositoryPort}, {@link LlmScoringPort}
 * и {@link SessionLlmRateLimiter}.
 */
@Component
public class AttemptScoringExecutor {

    private static final Logger logger = LoggerFactory.getLogger(AttemptScoringExecutor.class);

    private final AttemptRepositoryPort attemptRepositoryPort;
    private final LlmScoringPort llmScoringPort;
    private final SessionLlmRateLimiter sessionLlmRateLimiter;

    public AttemptScoringExecutor(AttemptRepositoryPort attemptRepositoryPort,
                                  LlmScoringPort llmScoringPort,
                                  SessionLlmRateLimiter sessionLlmRateLimiter) {
        this.attemptRepositoryPort = attemptRepositoryPort;
        this.llmScoringPort = llmScoringPort;
        this.sessionLlmRateLimiter = sessionLlmRateLimiter;
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
        logger.debug("Оценка ответа через LLM: sessionId={}, attemptId={}", attempt.getSessionId(), attempt.getId());
        ScoreResult result = sessionLlmRateLimiter.execute(attempt.getSessionId(),
                () -> llmScoringPort.score(attempt.getQuestionText(), attempt.getFinalTranscript()));
        logger.info("Ответ оценён: attemptId={}, score={}, confidence={}",
                attempt.getId(), result.getScore(), result.getConfidence());
        return attemptRepositoryPort.save(
                attempt.withScore(BigDecimal.valueOf(result.getScore()), result.getConfidence(),
                        result.isValid(), result.getFeedback()));
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
                    logger.error("Batch scoring failed for attempt {}", attempt.getId(), e);
                }
            } else if (attempt.getFinalTranscript() == null) {
                logger.debug("Пропущена попытка без транскрипта: attemptId={}", attempt.getId());
            }
        }
    }
}