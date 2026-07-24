package com.assessment.repository;

import com.assessment.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для управления темами внутри секций компетенций.
 */
public interface TopicRepository extends JpaRepository<Topic, UUID> {

    /**
     * Возвращает список тем, принадлежащих указанной секции.
     *
     * @param sectionId идентификатор секции
     * @return список тем
     */
    List<Topic> findBySectionId(UUID sectionId);
}
