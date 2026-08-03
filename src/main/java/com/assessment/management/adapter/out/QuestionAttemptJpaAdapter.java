package com.assessment.management.adapter.out;

import com.assessment.management.port.out.QuestionAttemptRepositoryPort;
import com.assessment.repository.QuestionAttemptRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * JPA-адаптер выходного порта удаления попыток ответов в management-контексте.
 *
 * <p>Оборачивает {@link QuestionAttemptRepository} и делегирует ему bulk-удаление
 * попыток по темам (самому разделу или компетенции) без дополнительной логики.
 */
@Component
public class QuestionAttemptJpaAdapter implements QuestionAttemptRepositoryPort {

    private final QuestionAttemptRepository questionAttemptRepository;

    public QuestionAttemptJpaAdapter(QuestionAttemptRepository questionAttemptRepository) {
        this.questionAttemptRepository = questionAttemptRepository;
    }

    @Override
    public void deleteByTopicId(UUID topicId) {
        questionAttemptRepository.deleteByTopicId(topicId);
    }

    @Override
    public void deleteBySectionId(UUID sectionId) {
        questionAttemptRepository.deleteBySectionId(sectionId);
    }

    @Override
    public void deleteByCompetencyId(UUID competencyId) {
        questionAttemptRepository.deleteByCompetencyId(competencyId);
    }
}
