package com.assessment.repository;

import com.assessment.entity.Competency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompetencyRepository extends JpaRepository<Competency, UUID> {
}
