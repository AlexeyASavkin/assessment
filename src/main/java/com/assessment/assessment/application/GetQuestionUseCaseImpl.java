package com.assessment.assessment.application;

import com.assessment.assessment.domain.AssessmentSession;
import com.assessment.assessment.domain.Attempt;
import com.assessment.assessment.domain.SessionStatus;
import com.assessment.assessment.domain.TopicInfo;
import com.assessment.assessment.port.out.AttemptRepositoryPort;
import com.assessment.assessment.port.out.SessionRepositoryPort;
import com.assessment.assessment.port.out.TopicQueryPort;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Реализация use case получения текущего вопроса сессии.
 *
 * <p>Возвращает уже назначенный сессии вопрос, либо создаёт новый из банка
 * вопросов следующей непройденной темы, либо сообщает о завершении сессии.
 * Зависит только от выходных портов и {@link SessionQuestionPicker}.
 */
@Service
public class GetQuestionUseCaseImpl implements GetQuestionUseCase {

    private final SessionRepositoryPort sessionRepositoryPort;
    private final AttemptRepositoryPort attemptRepositoryPort;
    private final TopicQueryPort topicQueryPort;
    private final SessionQuestionPicker questionPicker;

    public GetQuestionUseCaseImpl(SessionRepositoryPort sessionRepositoryPort,
                                  AttemptRepositoryPort attemptRepositoryPort,
                                  TopicQueryPort topicQueryPort,
                                  SessionQuestionPicker questionPicker) {
        this.sessionRepositoryPort = sessionRepositoryPort;
        this.attemptRepositoryPort = attemptRepositoryPort;
        this.topicQueryPort = topicQueryPort;
        this.questionPicker = questionPicker;
    }

    @Override
    public QuestionOutcome getCurrentQuestion(UUID sessionId) {
        AssessmentSession session = sessionRepositoryPort.findById(sessionId).orElseThrow();

        if (session.getStatus() == SessionStatus.COMPLETED) {
            return new QuestionOutcome.Completed();
        }

        UUID currentQuestionId = session.getCurrentQuestionId();
        if (currentQuestionId != null) {
            Optional<Attempt> current = attemptRepositoryPort.findById(currentQuestionId);
            if (current.isPresent()) {
                return new QuestionOutcome.Question(current.get());
            }
        }

        List<Attempt> attempts = attemptRepositoryPort.findBySessionIdOrderByCreatedAtAsc(sessionId);
        if (questionPicker.hasReachedQuestionLimit(attempts)) {
            return new QuestionOutcome.Completed();
        }
        List<TopicInfo> topics = topicsFor(session);
        UUID nextTopicId = questionPicker.findNextTopicId(attempts, topics);

        if (nextTopicId == null) {
            return new QuestionOutcome.Completed();
        }

        String questionText = questionPicker.pickQuestion(nextTopicId, attempts);

        Attempt attempt = attemptRepositoryPort.save(
                Attempt.of(null, sessionId, questionText, null, null, null, null, null, null,
                        0, null, nextTopicId, null, null, null, null));
        sessionRepositoryPort.save(session.withCurrentQuestionId(attempt.getId()));

        return new QuestionOutcome.Question(attempt);
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