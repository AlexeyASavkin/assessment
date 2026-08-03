package com.assessment.management.application;

import com.assessment.entity.Topic;
import com.assessment.management.port.out.QuestionAttemptRepositoryPort;
import com.assessment.management.port.out.SectionRepositoryPort;
import com.assessment.management.port.out.TopicRepositoryPort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Реализация use case CRUD-операций над темами.
 *
 * <p>Воспроизводит бизнес-логику {@code AdminController}: при создании тема
 * привязывается к найденному разделу (если его нет — возвращается пусто),
 * обновление применяет мутатор к управляемой сущности, удаление — по идентификатору.
 * Зависит от выходных портов {@link SectionRepositoryPort} и
 * {@link TopicRepositoryPort}.
 */
@Service
public class TopicCrudUseCaseImpl implements TopicCrudUseCase {

    private final SectionRepositoryPort sectionRepositoryPort;
    private final TopicRepositoryPort topicRepositoryPort;
    private final QuestionAttemptRepositoryPort questionAttemptRepositoryPort;

    public TopicCrudUseCaseImpl(SectionRepositoryPort sectionRepositoryPort,
                                TopicRepositoryPort topicRepositoryPort,
                                QuestionAttemptRepositoryPort questionAttemptRepositoryPort) {
        this.sectionRepositoryPort = sectionRepositoryPort;
        this.topicRepositoryPort = topicRepositoryPort;
        this.questionAttemptRepositoryPort = questionAttemptRepositoryPort;
    }

    @Override
    @Transactional
    public Optional<Topic> createTopic(UUID sectionId, Topic topic) {
        return sectionRepositoryPort.findById(sectionId)
                .map(section -> {
                    topic.setSection(section);
                    return topicRepositoryPort.save(topic);
                });
    }

    @Override
    public List<Topic> listTopics(UUID sectionId) {
        return topicRepositoryPort.findBySectionId(sectionId);
    }

    @Override
    @Transactional
    public Optional<Topic> updateTopic(UUID id, UnaryOperator<Topic> mutator) {
        return topicRepositoryPort.findById(id)
                .map(entity -> {
                    mutator.apply(entity);
                    return topicRepositoryPort.save(entity);
                });
    }

    @Override
    @Transactional
    public void deleteTopic(UUID id) {
        // Попытки ответов ссылаются на тему через FK fk_attempts_topic —
        // чистим их до удаления самой темы, иначе БД отклонит удаление.
        questionAttemptRepositoryPort.deleteByTopicId(id);
        topicRepositoryPort.deleteById(id);
    }
}
