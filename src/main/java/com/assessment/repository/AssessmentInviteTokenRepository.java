package com.assessment.repository;

import com.assessment.entity.AssessmentInviteToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для управления одноразовыми пригласительными токенами для прохождения оценки.
 */
public interface AssessmentInviteTokenRepository extends JpaRepository<AssessmentInviteToken, UUID> {

    /**
     * Находит пригласительный токен по хешу токена.
     *
     * @param tokenHash хеш токена
     * @return токен или пустой результат, если токен не найден
     */
    Optional<AssessmentInviteToken> findByTokenHash(String tokenHash);

    /**
     * Удаляет все пригласительные токены, связанные с указанным сотрудником.
     *
     * @param employeeId идентификатор сотрудника
     */
    @Modifying
    @Query("DELETE FROM AssessmentInviteToken t WHERE t.employee.id = :employeeId")
    void deleteByEmployeeId(@Param("employeeId") UUID employeeId);
}
