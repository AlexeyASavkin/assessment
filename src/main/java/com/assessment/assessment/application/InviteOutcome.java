package com.assessment.assessment.application;

import com.assessment.assessment.domain.AssessmentSession;

/**
 * Результат валидации пригласительного токена.
 *
 * @param session сессия оценки (существующая или вновь созданная)
 * @param reused  {@code true}, если сессия уже существовала (токен или ссылка открыты повторно)
 */
public record InviteOutcome(AssessmentSession session, boolean reused) {
}
