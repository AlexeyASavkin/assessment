package com.assessment.assessment.application;

import java.util.Optional;

/**
 * Use case валидации одноразового пригласительного токена сотрудника.
 *
 * <p>Проверяет HMAC-подпись токена и срок действия, при первом использовании
 * создаёт или находит сессию сотрудника и помечает токен как использованный.
 */
public interface InviteEmployeeUseCase {

    /**
     * Валидирует пригласительный токен и возвращает сессию оценки.
     *
     * @param token строковое значение пригласительного токена
     * @return результат валидации {@link InviteOutcome} или пусто, если токен невалиден или просрочен
     */
    Optional<InviteOutcome> validateInvite(String token);
}
