package com.assessment.management.adapter.out;

import com.assessment.entity.Topic;
import com.assessment.management.port.out.TopicRepositoryPort;
import com.assessment.repository.TopicRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-адаптер выходного порта доступа к темам разделов в management-контексте.
 *
 * <p>Оборачивает {@link TopicRepository} и делегирует ему вызовы без
 * дополнительной логики (сущность используется как есть).
 */
@Component
public class TopicJpaAdapter implements TopicRepositoryPort {

    private final TopicRepository topicRepository;

    public TopicJpaAdapter(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    @Override
    public Topic save(Topic topic) {
        return topicRepository.save(topic);
    }

    @Override
    public Optional<Topic> findById(UUID id) {
        return topicRepository.findById(id);
    }

    @Override
    public List<Topic> findBySectionId(UUID sectionId) {
        return topicRepository.findBySectionId(sectionId);
    }

    @Override
    public void deleteById(UUID id) {
        topicRepository.deleteById(id);
    }
}
