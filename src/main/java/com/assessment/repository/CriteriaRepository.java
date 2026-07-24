package com.assessment.repository;

import com.assessment.entity.Criteria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для управления критериями оценки внутри компетенций.
 */
public interface CriteriaRepository extends JpaRepository<Criteria, UUID> {

    /**
     * Возвращает список критериев, принадлежащих указанной компетенции.
     *
     * @param competencyId идентификатор компетенции
     * @return список критериев
     */
    List<Criteria> findByCompetencyId(UUID competencyId);
}
