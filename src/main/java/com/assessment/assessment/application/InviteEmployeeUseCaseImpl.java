package com.assessment.assessment.application;

import com.assessment.assessment.domain.AssessmentSession;
import com.assessment.assessment.domain.InviteToken;
import com.assessment.assessment.domain.SessionStatus;
import com.assessment.assessment.port.out.InviteTokenRepositoryPort;
import com.assessment.assessment.port.out.SessionRepositoryPort;
import com.assessment.security.HmacTokenValidator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Реализация use case валидации одноразового пригласительного токена сотрудника.
 *
 * <p>Проверяет HMAC-подпись токена и срок действия, при первом использовании
 * создаёт или находит сессию сотрудника и помечает токен как использованный.
 * Зависит от выходных портов и {@link HmacTokenValidator}.
 */
@Service
public class InviteEmployeeUseCaseImpl implements InviteEmployeeUseCase {

    private final InviteTokenRepositoryPort inviteTokenRepositoryPort;
    private final SessionRepositoryPort sessionRepositoryPort;
    private final HmacTokenValidator hmacValidator;

    public InviteEmployeeUseCaseImpl(InviteTokenRepositoryPort inviteTokenRepositoryPort,
                                     SessionRepositoryPort sessionRepositoryPort,
                                     HmacTokenValidator hmacValidator) {
        this.inviteTokenRepositoryPort = inviteTokenRepositoryPort;
        this.sessionRepositoryPort = sessionRepositoryPort;
        this.hmacValidator = hmacValidator;
    }

    /**
     * Валидирует пригласительный токен и создаёт/возвращает сессию сотрудника.
     *
     * <p>Операция read-modify-write (найти токен → создать сессию → пометить токен
     * использованным) выполняется в одной транзакции, чтобы исключить гонку при
     * одновременном открытии одной и той же пригласительной ссылки.
     *
     * @param token пригласительный токен
     * @return результат валидации или {@link Optional#empty()} если токен невалиден/истёк
     */
    @Override
    @Transactional
    public Optional<InviteOutcome> validateInvite(String token) {
        String hash = hmacValidator.generateToken(token);
        return inviteTokenRepositoryPort.findByTokenHash(hash)
                .filter(InviteToken::isNotExpired)
                .map(t -> {
                    // Токен уже использован и привязан к сессии — возвращаем существующую сессию
                    if (t.isUsed() && t.getSessionId() != null) {
                        AssessmentSession existing = sessionRepositoryPort.findById(t.getSessionId()).orElseThrow();
                        return new InviteOutcome(existing, true);
                    }

                    // Первое использование — ищем существующую сессию сотрудника или создаём новую
                    Optional<AssessmentSession> existing = sessionRepositoryPort
                            .findFirstByEmployeeIdOrderByCreatedAtDesc(t.getEmployeeId());

                    AssessmentSession session;
                    boolean reused;
                    if (existing.isPresent()) {
                        session = existing.get();
                        reused = true;
                    } else {
                        session = sessionRepositoryPort.save(
                                AssessmentSession.of(null, t.getEmployeeId(), null, null,
                                        SessionStatus.ACTIVE, null, null));
                        reused = false;
                    }

                    inviteTokenRepositoryPort.save(t.markUsed(session.getId()));
                    return new InviteOutcome(session, reused);
                });
    }
}