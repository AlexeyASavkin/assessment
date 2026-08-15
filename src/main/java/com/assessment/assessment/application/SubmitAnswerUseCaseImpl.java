package com.assessment.assessment.application;

import com.assessment.assessment.domain.AssessmentSession;
import com.assessment.assessment.domain.Attempt;
import com.assessment.assessment.domain.SessionStatus;
import com.assessment.assessment.domain.TopicInfo;
import com.assessment.assessment.port.out.AttemptRepositoryPort;
import com.assessment.assessment.port.out.SessionRepositoryPort;
import com.assessment.assessment.port.out.TopicQueryPort;
import com.assessment.ai.domain.FollowUpResult;
import com.assessment.ai.domain.ScoreResult;
import com.assessment.ai.port.LlmFollowUpPort;
import com.assessment.common.ForbiddenException;
import com.assessment.config.SessionLlmRateLimiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Реализация use case отправки ответа сотрудника.
 *
 * <p>Сохраняет транскрипт ответа, оценивает его через LLM (синхронно),
 * генерирует уточняющие вопросы для слабых ответов (глубина ≤ 1) и возвращает
 * следующий вопрос либо завершает сессию, когда все темы пройдены или достигнут
 * лимит вопросов сессии. LLM-вызовы уточнений ограничиваются персональным
 * bucket'ом сессии через {@link SessionLlmRateLimiter}. Зависит
 * только от выходных портов, AI-портов и вспомогательных компонентов
 * {@link AttemptScoringExecutor} и {@link SessionQuestionPicker}.
 */
@Service
public class SubmitAnswerUseCaseImpl implements SubmitAnswerUseCase {

    private static final Logger logger = LoggerFactory.getLogger(SubmitAnswerUseCaseImpl.class);

    private final SessionRepositoryPort sessionRepositoryPort;
    private final AttemptRepositoryPort attemptRepositoryPort;
    private final TopicQueryPort topicQueryPort;
    private final LlmFollowUpPort llmFollowUpPort;
    private final AttemptScoringExecutor scoringExecutor;
    private final SessionQuestionPicker questionPicker;
    private final SessionLlmRateLimiter sessionLlmRateLimiter;

    public SubmitAnswerUseCaseImpl(SessionRepositoryPort sessionRepositoryPort,
                                   AttemptRepositoryPort attemptRepositoryPort,
                                   TopicQueryPort topicQueryPort,
                                   LlmFollowUpPort llmFollowUpPort,
                                   AttemptScoringExecutor scoringExecutor,
                                   SessionQuestionPicker questionPicker,
                                   SessionLlmRateLimiter sessionLlmRateLimiter) {
        this.sessionRepositoryPort = sessionRepositoryPort;
        this.attemptRepositoryPort = attemptRepositoryPort;
        this.topicQueryPort = topicQueryPort;
        this.llmFollowUpPort = llmFollowUpPort;
        this.scoringExecutor = scoringExecutor;
        this.questionPicker = questionPicker;
        this.sessionLlmRateLimiter = sessionLlmRateLimiter;
    }

    @Override
    @Transactional
    public AnswerOutcome submitAnswer(UUID sessionId, UUID questionAttemptId, String finalTranscript) {
        logger.debug("Принят ответ: sessionId={}, questionAttemptId={}", sessionId, questionAttemptId);
        AssessmentSession session = sessionRepositoryPort.findById(sessionId).orElseThrow();

        // Завершённая сессия не принимает ответы — защита от повторной отправки после отчёта.
        if (session.getStatus() == SessionStatus.COMPLETED) {
            logger.warn("Попытка ответа в завершённую сессию отклонена: sessionId={}, questionAttemptId={}",
                    sessionId, questionAttemptId);
            throw new ForbiddenException("Сессия уже завершена");
        }

        Attempt currentAttempt = attemptRepositoryPort.findById(questionAttemptId).orElseThrow();

        // SEC: IDOR-защита — попытка должна принадлежать текущей сессии, иначе 403.
        if (!sessionId.equals(currentAttempt.getSessionId())) {
            logger.warn("IDOR-попытка: попытка {} не принадлежит сессии {}", questionAttemptId, sessionId);
            throw new ForbiddenException("Попытка ответа не принадлежит текущей сессии");
        }

        currentAttempt = attemptRepositoryPort.save(currentAttempt.withFinalTranscript(finalTranscript));
        logger.info("Сохранён транскрипт ответа: sessionId={}, attemptId={}", sessionId, currentAttempt.getId());

        List<TopicInfo> topics = topicsFor(session);
        UUID currentTopicId = currentAttempt.getTopicId();
        int currentDepth = currentAttempt.getFollowupDepth() != null ? currentAttempt.getFollowupDepth() : 0;
        List<Attempt> allAttempts = attemptRepositoryPort.findBySessionIdOrderByCreatedAtAsc(sessionId);
        boolean limitReached = questionPicker.hasReachedQuestionLimit(allAttempts);

        if (currentDepth > 0) {
            // Оценка логируется внутри AttemptScoringExecutor.scoreNow (attemptId, score, confidence)
            Attempt scored = scoringExecutor.scoreNow(currentAttempt);

            if (currentAttempt.getFollowupParentId() != null) {
                Attempt parent = attemptRepositoryPort.findById(currentAttempt.getFollowupParentId()).orElse(null);
                if (parent != null) {
                    rescoreMainAttempt(parent, scored);
                }
            }

            if (currentTopicId != null && !limitReached) {
                Attempt candidate = findFollowUpCandidate(sessionId, currentTopicId);
                if (candidate != null) {
                    String followUpQuestion = generateFollowUp(candidate);
                    if (followUpQuestion != null && !followUpQuestion.isBlank()) {
                        // Глубина уточнения = глубина кандидата + 1. Кандидаты — только основные
                        // попытки (depth 0), поэтому уточнение всегда depth=1: цепочка не растёт.
                        Attempt followUpAttempt = attemptRepositoryPort.save(
                                Attempt.of(null, sessionId, followUpQuestion, null, null, null, null, null, null,
                                        candidate.getFollowupDepth() + 1, candidate.getId(), candidate.getTopicId(),
                                        null, null, null, null));
                        sessionRepositoryPort.save(session.withCurrentQuestionId(followUpAttempt.getId()));
                        logger.info("Создан уточняющий вопрос: sessionId={}, followUpAttemptId={}, parentId={}, depth={}",
                                sessionId, followUpAttempt.getId(), candidate.getId(), candidate.getFollowupDepth() + 1);
                        return new AnswerOutcome.NextQuestion(followUpAttempt, currentTopicId);
                    }
                }
            }

            return advanceToNextTopicOrComplete(session, topics);
        }

        boolean topicHasMore = currentTopicId != null && !limitReached
                && questionPicker.hasUnused(currentTopicId, allAttempts);

        if (topicHasMore) {
            // Оценка логируется внутри AttemptScoringExecutor.scoreNow (attemptId, score, confidence)
            scoringExecutor.scoreNow(currentAttempt);
            String nextQuestionText = questionPicker.pickQuestion(currentTopicId, allAttempts);
            Attempt nextAttempt = attemptRepositoryPort.save(
                    Attempt.of(null, sessionId, nextQuestionText, null, null, null, null, null, null,
                            0, null, currentTopicId, null, null, null, null));
            sessionRepositoryPort.save(session.withCurrentQuestionId(nextAttempt.getId()));
            logger.info("Выдан следующий вопрос: sessionId={}, attemptId={}, topicId={}",
                    sessionId, nextAttempt.getId(), currentTopicId);
            return new AnswerOutcome.NextQuestion(nextAttempt, currentTopicId);
        }

        scoringExecutor.scoreUnscored(sessionId);

        if (currentTopicId != null && !limitReached) {
            Attempt candidate = findFollowUpCandidate(sessionId, currentTopicId);
            if (candidate != null) {
                String followUpQuestion = generateFollowUp(candidate);
                if (followUpQuestion != null && !followUpQuestion.isBlank()) {
                    Attempt followUpAttempt = attemptRepositoryPort.save(
                            Attempt.of(null, sessionId, followUpQuestion, null, null, null, null, null, null,
                                    candidate.getFollowupDepth() + 1, candidate.getId(), candidate.getTopicId(), null, null, null, null));
                    sessionRepositoryPort.save(session.withCurrentQuestionId(followUpAttempt.getId()));
                    logger.info("Создан уточняющий вопрос: sessionId={}, followUpAttemptId={}, parentId={}, depth={}",
                            sessionId, followUpAttempt.getId(), candidate.getId(), candidate.getFollowupDepth() + 1);
                    return new AnswerOutcome.NextQuestion(followUpAttempt, currentTopicId);
                }
            }
        }

        return advanceToNextTopicOrComplete(session, topics);
    }

    /**
     * Подбирает следующий основной вопрос из следующей темы либо завершает сессию.
     *
     * @param session текущая сессия
     * @param topics  список тем компетенции сотрудника
     * @return следующий вопрос или признак завершения
     */
    private AnswerOutcome advanceToNextTopicOrComplete(AssessmentSession session, List<TopicInfo> topics) {
        List<Attempt> allAttempts = attemptRepositoryPort.findBySessionIdOrderByCreatedAtAsc(session.getId());

        if (questionPicker.hasReachedQuestionLimit(allAttempts)) {
            scoringExecutor.scoreUnscored(session.getId());
            sessionRepositoryPort.save(session.withStatus(SessionStatus.COMPLETED));
            logger.info("Сессия завершена (достигнут лимит вопросов): sessionId={}", session.getId());
            return new AnswerOutcome.Completed();
        }

        UUID nextTopicId = questionPicker.findNextTopicId(allAttempts, topics);

        if (nextTopicId == null) {
            scoringExecutor.scoreUnscored(session.getId());
            sessionRepositoryPort.save(session.withStatus(SessionStatus.COMPLETED));
            logger.info("Сессия завершена (все темы пройдены): sessionId={}", session.getId());
            return new AnswerOutcome.Completed();
        }

        String nextQuestionText = questionPicker.pickQuestion(nextTopicId, allAttempts);
        Attempt nextAttempt = attemptRepositoryPort.save(
                Attempt.of(null, session.getId(), nextQuestionText, null, null, null, null, null, null,
                        0, null, nextTopicId, null, null, null, null));
        sessionRepositoryPort.save(session.withCurrentQuestionId(nextAttempt.getId()));
        logger.info("Переход к следующей теме: sessionId={}, topicId={}, attemptId={}",
                session.getId(), nextTopicId, nextAttempt.getId());

        return new AnswerOutcome.NextQuestion(nextAttempt, nextAttempt.getTopicId());
    }

    /**
     * Переоценивает основную попытку с учётом ответа на уточняющий вопрос.
     *
     * <p>Сохраняет {@code baseScore} = исходный {@code score} (только если он ещё
     * не задан). При сбое LLM оставляет оценку без изменений.
     *
     * @param mainAttempt     основная попытка (depth=0)
     * @param followUpAttempt уточняющая попытка (depth&gt;0)
     */
    private void rescoreMainAttempt(Attempt mainAttempt, Attempt followUpAttempt) {
        if (mainAttempt.getBaseScore() == null && mainAttempt.getScore() != null) {
            mainAttempt = mainAttempt.withBaseScore(mainAttempt.getScore());
        }

        final Attempt attempt = mainAttempt;
        Optional<ScoreResult> result = sessionLlmRateLimiter.execute(attempt.getSessionId(),
                () -> llmFollowUpPort.rescoreMainAttempt(
                        attempt.getQuestionText(),
                        attempt.getFinalTranscript(),
                        followUpAttempt.getQuestionText(),
                        followUpAttempt.getFinalTranscript()));
        if (result.isEmpty()) {
            return;
        }

        ScoreResult r = result.get();
        Attempt updated = mainAttempt.withScore(BigDecimal.valueOf(r.getScore()), r.getConfidence(),
                r.isValid(), r.getFeedback());
        if (!r.getFeedback().isEmpty()) {
            updated = updated.withScore(updated.getScore(), updated.getConfidence(),
                    updated.getValidJudge(), r.getFeedback());
        }
        attemptRepositoryPort.save(updated);
    }

    /**
     * Генерирует уточняющий вопрос для кандидата через LLM.
     *
     * @param candidate основная попытка с оценкой ≤ 2
     * @return текст уточняющего вопроса или {@code null} при сбое LLM
     */
    private String generateFollowUp(Attempt candidate) {
        return sessionLlmRateLimiter.execute(candidate.getSessionId(),
                () -> llmFollowUpPort.generateFollowUpQuestion(candidate.getQuestionText(), candidate.getFinalTranscript())
                        .map(FollowUpResult::getQuestionText)
                        .orElse(null));
    }

    /**
     * Находит первую (хронологически) основную попытку в теме, подходящую для уточнения.
     *
     * <p>Критерии: тема совпадает, {@code followupDepth == 0},
     * {@code validJudge == true}, {@code score != null}, {@code score ≤ 2},
     * нет существующего child-уточнения.
     *
     * @param sessionId идентификатор сессии
     * @param topicId   идентификатор темы
     * @return кандидат или {@code null}
     */
    private Attempt findFollowUpCandidate(UUID sessionId, UUID topicId) {
        List<Attempt> attempts = attemptRepositoryPort.findBySessionIdOrderByCreatedAtAsc(sessionId);

        Set<UUID> parentsWithChild = attempts.stream()
                .map(Attempt::getFollowupParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Attempt a : attempts) {
            if (a.getTopicId() == null || !a.getTopicId().equals(topicId)) {
                continue;
            }
            if (a.getFollowupDepth() != null && a.getFollowupDepth() > 0) {
                continue;
            }
            if (a.getValidJudge() == null || !a.getValidJudge()) {
                continue;
            }
            if (a.getScore() == null) {
                continue;
            }
            if (a.getScore().compareTo(BigDecimal.valueOf(2)) > 0) {
                continue;
            }
            if (parentsWithChild.contains(a.getId())) {
                continue;
            }
            return a;
        }
        return null;
    }

    /**
     * Возвращает темы, отфильтрованные по компетенции сотрудника, либо все темы.
     *
     * @param session текущая сессия
     * @return список тем для оценки
     */
    private List<TopicInfo> topicsFor(AssessmentSession session) {
        List<TopicInfo> all = topicQueryPort.findAll();
        if (session.getCompetencyId() == null) {
            return all;
        }
        UUID competencyId = session.getCompetencyId();
        return all.stream().filter(t -> competencyId.equals(t.getCompetencyId())).toList();
    }
}