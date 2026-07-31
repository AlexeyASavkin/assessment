package com.assessment.assessment.port.out;

import com.assessment.assessment.domain.AssessmentSession;

import java.util.Optional;
import java.util.UUID;

/**
 * Выходной порт доступа к сессиям оценки.
 *
 * <p>Реализуется JPA-адаптером поверх {@code SessionRepository}.
 */
public interface SessionRepositoryPort {

    Optional<AssessmentSession> findById(UUID sessionId);

    AssessmentSession save(AssessmentSession session);

    /**
     * Возвращает самую свежую сессию сотрудника (по дате создания).
     *
     * @param employeeId идентификатор сотрудника
     * @return последняя сессия сотрудника, если есть
     */
    Optional<AssessmentSession> findFirstByEmployeeIdOrderByCreatedAtDesc(UUID employeeId);
}
