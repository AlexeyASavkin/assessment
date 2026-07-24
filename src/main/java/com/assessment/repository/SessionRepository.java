package com.assessment.repository;

import com.assessment.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    List<Session> findByEmployeeId(UUID employeeId);

    Optional<Session> findFirstByEmployeeIdOrderByCreatedAtDesc(UUID employeeId);
}
