package com.assessment.repository;

import com.assessment.entity.Competency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Репозиторий для управления компетенциями, по которым проводится оценка сотрудников.
 */
public interface CompetencyRepository extends JpaRepository<Competency, UUID> {
}
