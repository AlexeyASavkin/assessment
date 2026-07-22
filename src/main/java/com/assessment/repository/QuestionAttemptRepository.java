package com.assessment.repository;

import com.assessment.entity.QuestionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestionAttemptRepository extends JpaRepository<QuestionAttempt, UUID> {
    List<QuestionAttempt> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);
    List<QuestionAttempt> findByTopicId(UUID topicId);
}
