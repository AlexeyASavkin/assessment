package com.assessment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Краткая сводка по заявке на оценку компетенций.
 * Связывает пригласительный токен, сессию и агрегированные метрики результата.
 *
 * @param tokenId           идентификатор пригласительного токена
 * @param employeeId        идентификатор сотрудника
 * @param employeeName      ФИО сотрудника
 * @param competencyName    наименование компетенции
 * @param sessionStatus     статус сессии (ACTIVE, COMPLETED или null, если сессия ещё не создана)
 * @param sessionId         идентификатор сессии (null, если сессия ещё не создана)
 * @param averageScore      средний балл по валидным попыткам (null, если оценок нет)
 * @param passed            результат прохождения (средний балл >= 3.0, false если оценка не завершена)
 * @param createdAt         дата создания пригласительного токена
 * @param completedAt       дата завершения сессии (null, если не завершена)
 */
public record ApplicationSummary(
        UUID tokenId,
        UUID employeeId,
        String employeeName,
        String competencyName,
        String sessionStatus,
        UUID sessionId,
        BigDecimal averageScore,
        boolean passed,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
}