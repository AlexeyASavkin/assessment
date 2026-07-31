package com.assessment.assessment.adapter.out;

import com.assessment.assessment.domain.Attempt;
import com.assessment.assessment.port.out.AttemptRepositoryPort;
import com.assessment.entity.QuestionAttempt;
import com.assessment.entity.Session;
import com.assessment.entity.Topic;
import com.assessment.repository.QuestionAttemptRepository;
import com.assessment.repository.SessionRepository;
import com.assessment.repository.TopicRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-адаптер выходного порта доступа к попыткам ответов.
 *
 * <p>Оборачивает {@link QuestionAttemptRepository}, {@link SessionRepository}
 * и {@link TopicRepository}, преобразуя сущность {@link QuestionAttempt} в
 * доменную модель {@link Attempt}. Чтение ленивых ассоциаций
 * (session, topic, section, competency) выполняется в транзакции.
 */
@Component
public class JpaAttemptRepositoryAdapter implements AttemptRepositoryPort {

    private final QuestionAttemptRepository questionAttemptRepository;
    private final SessionRepository sessionRepository;
    private final TopicRepository topicRepository;

    public JpaAttemptRepositoryAdapter(QuestionAttemptRepository questionAttemptRepository,
                                       SessionRepository sessionRepository,
                                       TopicRepository topicRepository) {
        this.questionAttemptRepository = questionAttemptRepository;
        this.sessionRepository = sessionRepository;
        this.topicRepository = topicRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Attempt> findById(UUID attemptId) {
        return questionAttemptRepository.findById(attemptId).map(this::toDomain);
    }

    @Override
    @Transactional
    public Attempt save(Attempt attempt) {
        QuestionAttempt entity;
        if (attempt.getId() != null) {
            entity = questionAttemptRepository.findById(attempt.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "QuestionAttempt not found: " + attempt.getId()));
            entity.setFinalTranscript(attempt.getFinalTranscript());
            entity.setScore(attempt.getScore());
            entity.setBaseScore(attempt.getBaseScore());
            entity.setConfidence(attempt.getConfidence());
            entity.setValidJudge(attempt.getValidJudge());
            entity.setFeedback(attempt.getFeedback());
        } else {
            entity = QuestionAttempt.builder()
                    .session(sessionRepository.getReferenceById(attempt.getSessionId()))
                    .questionText(attempt.getQuestionText())
                    .topic(attempt.getTopicId() != null
                            ? topicRepository.getReferenceById(attempt.getTopicId())
                            : null)
                    .followupDepth(attempt.getFollowupDepth() != null ? attempt.getFollowupDepth() : 0)
                    .followupParent(attempt.getFollowupParentId() != null
                            ? questionAttemptRepository.getReferenceById(attempt.getFollowupParentId())
                            : null)
                    .build();
        }
        QuestionAttempt saved = questionAttemptRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attempt> findBySessionIdOrderByCreatedAtAsc(UUID sessionId) {
        return questionAttemptRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(this::toDomain)
                .toList();
    }

    private Attempt toDomain(QuestionAttempt e) {
        Session session = e.getSession();
        Topic topic = e.getTopic();
        String topicName = null;
        String sectionName = null;
        String competencyName = null;
        if (topic != null) {
            topicName = topic.getName();
            if (topic.getSection() != null) {
                sectionName = topic.getSection().getName();
                if (topic.getSection().getCompetency() != null) {
                    competencyName = topic.getSection().getCompetency().getName();
                }
            }
        }
        return Attempt.of(
                e.getId(),
                session.getId(),
                e.getQuestionText(),
                e.getFinalTranscript(),
                e.getScore(),
                e.getBaseScore(),
                e.getConfidence(),
                e.getValidJudge(),
                e.getFeedback(),
                e.getFollowupDepth(),
                e.getFollowupParent() != null ? e.getFollowupParent().getId() : null,
                topic != null ? topic.getId() : null,
                topicName,
                sectionName,
                competencyName,
                e.getCreatedAt());
    }
}