package com.assessment.repository;

import com.assessment.entity.AssessmentInviteToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AssessmentInviteTokenRepository extends JpaRepository<AssessmentInviteToken, UUID> {
    Optional<AssessmentInviteToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("DELETE FROM AssessmentInviteToken t WHERE t.employee.id = :employeeId")
    void deleteByEmployeeId(@Param("employeeId") UUID employeeId);
}
