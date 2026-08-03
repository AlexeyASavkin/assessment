package com.assessment.management.application;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Входной порт (use case) формирования сводки заявок на оценку
 * и админского отчёта по сессии.
 *
 * <p>Сводка заявок строится на основе выданных пригласительных токенов:
 * для каждой заявки агрегируются данные сотрудника, сессии и результата.
 * Для завершённых сессий средний балл и прохождение считаются через
 * сервис отчётов.
 */
public interface ApplicationManagementUseCase {

    /**
     * Возвращает список всех заявок на оценку.
     *
     * @return список сводок заявок
     */
    List<ApplicationSummary> listApplications();

    /**
     * Формирует развёрнутый отчёт по сессии для админ-панели.
     *
     * @param sessionId идентификатор сессии
     * @return карта с полным отчётом или пусто, если сессия не найдена
     */
    Optional<Map<String, Object>> getAdminReport(UUID sessionId);
}
