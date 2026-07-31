package com.assessment.management.port.out;

import com.assessment.entity.AssessmentInviteToken;

import java.util.List;
import java.util.UUID;

/**
 * Выходной порт доступа к сущности {@link AssessmentInviteToken} в management-контексте.
 *
 * <p>Реализуется JPA-адаптером поверх Spring Data репозитория.
 * Сущность используется как есть (pure-CRUD, без отдельной доменной модели).
 */
public interface TokenRepositoryPort {

    /**
     * Сохраняет (создаёт или обновляет) пригласительный токен.
     *
     * @param token токен
     * @return сохранённый токен
     */
    AssessmentInviteToken save(AssessmentInviteToken token);

    /**
     * Возвращает все выданные пригласительные токены.
     *
     * @return список токенов
     */
    List<AssessmentInviteToken> findAll();

    /**
     * Удаляет все токены указанного сотрудника.
     *
     * @param employeeId идентификатор сотрудника
     */
    void deleteByEmployeeId(UUID employeeId);
}
