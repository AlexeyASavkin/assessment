package com.assessment.repository;

import com.assessment.entity.CriteriaLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CriteriaLevelRepository extends JpaRepository<CriteriaLevel, UUID> {
    List<CriteriaLevel> findByCriteriaId(UUID criteriaId);
}
