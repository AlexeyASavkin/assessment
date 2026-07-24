package com.assessment.repository;

import com.assessment.entity.CriteriaLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Репозиторий для управления уровнями требований критериев оценки (JUNIOR, MIDDLE, SENIOR).
 */
public interface CriteriaLevelRepository extends JpaRepository<CriteriaLevel, UUID> {

    /**
     * Возвращает список уровней требований для указанного критерия.
     *
     * @param criteriaId идентификатор критерия
     * @return список уровней требований
     */
    List<CriteriaLevel> findByCriteriaId(UUID criteriaId);
}
