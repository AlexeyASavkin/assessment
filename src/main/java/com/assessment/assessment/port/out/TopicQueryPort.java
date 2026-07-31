package com.assessment.assessment.port.out;

import com.assessment.assessment.domain.TopicInfo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Выходной порт доступа к темам оценки.
 *
 * <p>Реализуется JPA-адаптером поверх {@code TopicRepository}.
 */
public interface TopicQueryPort {

    List<TopicInfo> findAll();

    Optional<TopicInfo> findById(UUID topicId);
}
