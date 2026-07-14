package com.assessment.repository;

import com.assessment.entity.Criteria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CriteriaRepository extends JpaRepository<Criteria, UUID> {
    List<Criteria> findByCompetencyId(UUID competencyId);
}
