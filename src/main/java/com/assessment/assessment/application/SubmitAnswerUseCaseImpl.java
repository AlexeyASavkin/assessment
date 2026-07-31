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

import org.springframework.stereotype.Service;

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
 * <p>Сохраняет транскрипт ответа, при необходимости оценивает его через LLM
 * (синхронно для уточняющих вопросов, асинхронно для основных), генерирует
 * уточняющие вопросы для слабых ответов и возвращает следующий вопрос либо
 * завершает сессию, когда все темы пройдены. Зависит только от выходных портов,
 * AI-портов и вспомогательных компонентов {@link AttemptScoringExecutor} и
 * {@link SessionQuestionPicker}.
 */
@Service
public class SubmitAnswerUseCaseImpl implements SubmitAnswerUseCase {

    private final SessionRepositoryPort sessionRepositoryPort;
    private final AttemptRepositoryPort attemptRepositoryPort;
    private final TopicQueryPort topicQueryPort;
    private final LlmFollowUpPort llmFollowUpPort;
    private final AttemptScoringExecutor scoringExecutor;
    private final SessionQuestionPicker questionPicker;

    public SubmitAnswerUseCaseImpl(SessionRepositoryPort sessionRepositoryPort,
                                   AttemptRepositoryPort attemptRepositoryPort,
                                   TopicQueryPort topicQueryPort,
                                   LlmFollowUpPort llmFollowUpPort,
                                   AttemptScoringExecutor scoringExecutor,
                                   SessionQuestionPicker questionPicker) {
        this.sessionRepositoryPort = sessionRepositoryPort;
        this.attemptRepositoryPort = attemptRepositoryPort;
        this.topicQueryPort = topicQueryPort;
        this.llmFollowUpPort = llmFollowUpPort;
        this.scoringExecutor = scoringExecutor;
        this.questionPicker = questionPicker;
    }

    @Override
    public AnswerOutcome submitAnswer(UUID sessionId, UUID questionAttemptId, String finalTranscript) {
        AssessmentSession session = sessionRepositoryPort.findById(sessionId).orElseThrow();
        Attempt currentAttempt = attemptRepositoryPort.findById(questionAttemptId).orElseThrow();
        currentAttempt = attemptRepositoryPort.save(currentAttempt.withFinalTranscript(finalTranscript));

        List<TopicInfo> topics = topicsFor(session);
        UUID currentTopicId = currentAttempt.getTopicId();
        int currentDepth = currentAttempt.getFollowupDepth() != null ? currentAttempt.getFollowupDepth() : 0;

        if (currentDepth > 0) {
            Attempt scored = scoringExecutor.scoreNow(currentAttempt);

            if (currentAttempt.getFollowupParentId() != null) {
                Attempt parent = attemptRepositoryPort.findById(currentAttempt.getFollowupParentId()).orElse(null);
                if (parent != null) {
                    rescoreMainAttempt(parent, scored);
                }
            }

            if (currentTopicId != null) {
                Attempt candidate = findFollowUpCandidate(sessionId, currentTopicId);
                if (candidate != null) {
                    String followUpQuestion = generateFollowUp(candidate);
                    if (followUpQuestion != null && !followUpQuestion.isBlank()) {
                        Attempt followUpAttempt = attemptRepositoryPort.save(
                                Attempt.of(null, sessionId, followUpQuestion, null, null, null, null, null, null,
                                        currentDepth + 1, candidate.getId(), candidate.getTopicId(),
                                        null, null, null, null));
                        sessionRepositoryPort.save(session.withCurrentQuestionId(followUpAttempt.getId()));
                        return new AnswerOutcome.NextQuestion(followUpAttempt, currentTopicId);
                    }
                }
            }

            return advanceToNextTopicOrComplete(session, topics);
        }

        List<Attempt> allAttempts = attemptRepositoryPort.findBySessionIdOrderByCreatedAtAsc(sessionId);
        boolean topicHasMore = currentTopicId != null && questionPicker.hasUnused(currentTopicId, allAttempts);

        if (topicHasMore) {
            scoringExecutor.scoreAsync(currentAttempt.getId());
            String nextQuestionText = questionPicker.pickQuestion(currentTopicId, allAttempts);
            Attempt nextAttempt = attemptRepositoryPort.save(
                    Attempt.of(null, sessionId, nextQuestionText, null, null, null, null, null, null,
                            0, null, currentTopicId, null, null, null, null));
            sessionRepositoryPort.save(session.withCurrentQuestionId(nextAttempt.getId()));
            return new AnswerOutcome.NextQuestion(nextAttempt, currentTopicId);
        }

        scoringExecutor.scoreUnscored(sessionId);

        if (currentTopicId != null) {
            Attempt candidate = findFollowUpCandidate(sessionId, currentTopicId);
            if (candidate != null) {
                String followUpQuestion = generateFollowUp(candidate);
                if (followUpQuestion != null && !followUpQuestion.isBlank()) {
                    Attempt followUpAttempt = attemptRepositoryPort.save(
                            Attempt.of(null, sessionId, followUpQuestion, null, null, null, null, null, null,
                                    1, candidate.getId(), candidate.getTopicId(), null, null, null, null));
                    sessionRepositoryPort.save(session.withCurrentQuestionId(followUpAttempt.getId()));
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
        UUID nextTopicId = questionPicker.findNextTopicId(allAttempts, topics);

        if (nextTopicId == null) {
            scoringExecutor.scoreUnscored(session.getId());
            sessionRepositoryPort.save(session.withStatus(SessionStatus.COMPLETED));
            return new AnswerOutcome.Completed();
        }

        String nextQuestionText = questionPicker.pickQuestion(nextTopicId, allAttempts);
        Attempt nextAttempt = attemptRepositoryPort.save(
                Attempt.of(null, session.getId(), nextQuestionText, null, null, null, null, null, null,
                        0, null, nextTopicId, null, null, null, null));
        sessionRepositoryPort.save(session.withCurrentQuestionId(nextAttempt.getId()));

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

        Optional<ScoreResult> result = llmFollowUpPort.rescoreMainAttempt(
                mainAttempt.getQuestionText(),
                mainAttempt.getFinalTranscript(),
                followUpAttempt.getQuestionText(),
                followUpAttempt.getFinalTranscript());
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
        return llmFollowUpPort.generateFollowUpQuestion(candidate.getQuestionText(), candidate.getFinalTranscript())
                .map(FollowUpResult::getQuestionText)
                .orElse(null);
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