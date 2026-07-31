package com.assessment.management.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Сводка заявки на оценку для админ-панели.
 *
 * <p>Связывает пригласительный токен, сотрудника, сессию и агрегированные
 * метрики результата (средний балл и прохождение). Возвращается use case
 * {@link ApplicationManagementUseCase} и преобразуется в API-DTO на уровне адаптера.
 *
 * @param tokenId         идентификатор пригласительного токена
 * @param employeeId      идентификатор сотрудника
 * @param employeeName    ФИО сотрудника
 * @param competencyName  название компетенции сотрудника
 * @param sessionStatus   статус сессии (ACTIVE, COMPLETED) или {@code null}
 * @param sessionId       идентификатор сессии или {@code null}
 * @param averageScore    средний балл по завершённой сессии или {@code null}
 * @param passed          результат прохождения (по завершённой сессии)
 * @param createdAt       дата выдачи токена
 * @param completedAt     дата завершения сессии или {@code null}
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
        Instant createdAt,
        Instant completedAt) {
}
