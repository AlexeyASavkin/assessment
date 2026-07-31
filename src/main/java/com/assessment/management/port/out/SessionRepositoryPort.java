package com.assessment.management.port.out;

import com.assessment.entity.Session;

import java.util.List;
import java.util.UUID;

/**
 * Выходной порт доступа к сущности {@link Session} в management-контексте.
 *
 * <p>Реализуется JPA-адаптером поверх Spring Data репозитория.
 * Нужен management-контексту для каскадного удаления сессий сотрудника
 * и проверки существования сессии при формировании админского отчёта.
 */
public interface SessionRepositoryPort {

    /**
     * Проверяет существование сессии по идентификатору.
     *
     * @param id идентификатор сессии
     * @return {@code true}, если сессия существует
     */
    boolean existsById(UUID id);

    /**
     * Возвращает сессии указанного сотрудника.
     *
     * @param employeeId идентификатор сотрудника
     * @return список сессий
     */
    List<Session> findByEmployeeId(UUID employeeId);

    /**
     * Удаляет сессию по идентификатору.
     *
     * @param id идентификатор сессии
     */
    void deleteById(UUID id);
}
