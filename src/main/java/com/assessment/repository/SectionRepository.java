package com.assessment.repository;

import com.assessment.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для управления секциями внутри компетенций.
 */
public interface SectionRepository extends JpaRepository<Section, UUID> {

    /**
     * Возвращает список секций, принадлежащих указанной компетенции.
     *
     * @param competencyId идентификатор компетенции
     * @return список секций
     */
    List<Section> findByCompetencyId(UUID competencyId);
}
