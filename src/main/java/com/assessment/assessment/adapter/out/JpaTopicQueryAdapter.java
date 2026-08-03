package com.assessment.assessment.adapter.out;

import com.assessment.assessment.domain.TopicInfo;
import com.assessment.assessment.port.out.TopicQueryPort;
import com.assessment.entity.Competency;
import com.assessment.entity.Section;
import com.assessment.entity.Topic;
import com.assessment.repository.TopicRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-адаптер выходного порта доступа к темам оценки.
 *
 * <p>Оборачивает {@link TopicRepository} и преобразует сущность {@link Topic}
 * в доменную модель {@link TopicInfo}. Чтение ленивых ассоциаций
 * (section, competency) выполняется в транзакции.
 */
@Component
public class JpaTopicQueryAdapter implements TopicQueryPort {

    private final TopicRepository topicRepository;

    public JpaTopicQueryAdapter(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopicInfo> findAll() {
        return topicRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TopicInfo> findById(UUID topicId) {
        return topicRepository.findById(topicId).map(this::toDomain);
    }

    private TopicInfo toDomain(Topic t) {
        Section section = t.getSection();
        String sectionName = null;
        UUID competencyId = null;
        String competencyName = null;
        if (section != null) {
            sectionName = section.getName();
            Competency competency = section.getCompetency();
            if (competency != null) {
                competencyId = competency.getId();
                competencyName = competency.getName();
            }
        }
        return TopicInfo.of(
                t.getId(),
                t.getName(),
                sectionName,
                competencyId,
                competencyName);
    }
}