package com.assessment.management.application;

import com.assessment.entity.AssessmentInviteToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Входной порт (use case) управления одноразовыми пригласительными токенами.
 *
 * <p>Генерирует HMAC-подписанную пригласительную ссылку для сотрудника и
 * предоставляет список всех выданных токенов. Хранит в БД только хеш токена.
 */
public interface TokenManagementUseCase {

    /**
     * Генерирует одноразовую пригласительную ссылку для сотрудника.
     * Предыдущие токены сотрудника удаляются, чтобы избежать нарушения
     * уникального ограничения.
     *
     * @param employeeId идентификатор сотрудника
     * @return относительный URL приглашения вида {@code /api/employee/invite/{token}}
     *         или пусто, если сотрудник не найден
     */
    Optional<String> generateInviteLink(UUID employeeId);

    /**
     * Возвращает список всех выданных пригласительных токенов.
     *
     * @return список токенов
     */
    List<AssessmentInviteToken> listTokens();
}
