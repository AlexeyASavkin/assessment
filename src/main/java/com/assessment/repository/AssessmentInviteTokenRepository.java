package com.assessment.repository;

import com.assessment.entity.AssessmentInviteToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AssessmentInviteTokenRepository extends JpaRepository<AssessmentInviteToken, UUID> {
    Optional<AssessmentInviteToken> findByTokenHash(String tokenHash);
}
