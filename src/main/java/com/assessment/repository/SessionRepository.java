package com.assessment.repository;

import com.assessment.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для управления сессиями оценки компетенций сотрудников.
 */
public interface SessionRepository extends JpaRepository<Session, UUID> {

    /**
     * Находит все сессии оценки, принадлежащие указанному сотруднику.
     *
     * @param employeeId идентификатор сотрудника
     * @return список сессий
     */
    List<Session> findByEmployeeId(UUID employeeId);

    /**
     * Находит последнюю созданную сессию оценки для указанного сотрудника.
     *
     * @param employeeId идентификатор сотрудника
     * @return последняя сессия или пустой результат, если сессий нет
     */
    Optional<Session> findFirstByEmployeeIdOrderByCreatedAtDesc(UUID employeeId);
}
