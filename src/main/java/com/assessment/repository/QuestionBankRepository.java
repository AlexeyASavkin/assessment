package com.assessment.repository;

import com.assessment.entity.QuestionBank;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface QuestionBankRepository extends JpaRepository<QuestionBank, UUID> {
    List<QuestionBank> findByCompetencyIdOrderByCreatedAtDesc(UUID competencyId);
}
