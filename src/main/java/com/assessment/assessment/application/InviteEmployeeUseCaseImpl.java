package com.assessment.assessment.application;

import com.assessment.assessment.domain.AssessmentSession;
import com.assessment.assessment.domain.InviteToken;
import com.assessment.assessment.domain.SessionStatus;
import com.assessment.assessment.port.out.InviteTokenRepositoryPort;
import com.assessment.assessment.port.out.SessionRepositoryPort;
import com.assessment.security.TokenHasher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Реализация use case валидации одноразового пригласительного токена сотрудника.
 *
 * <p>Вычисляет SHA-256 хеш предоставленного токена и ищет его в БД, проверяет
 * срок действия, при первом использовании создаёт или находит сессию сотрудника
 * и помечает токен как использованный. Повторное использование токена
 * отклоняется. Зависит от выходных портов и {@link TokenHasher}.
 */
@Service
public class InviteEmployeeUseCaseImpl implements InviteEmployeeUseCase {

    private static final Logger logger = LoggerFactory.getLogger(InviteEmployeeUseCaseImpl.class);

    private final InviteTokenRepositoryPort inviteTokenRepositoryPort;
    private final SessionRepositoryPort sessionRepositoryPort;
    private final TokenHasher tokenHasher;

    public InviteEmployeeUseCaseImpl(InviteTokenRepositoryPort inviteTokenRepositoryPort,
                                     SessionRepositoryPort sessionRepositoryPort,
                                     TokenHasher tokenHasher) {
        this.inviteTokenRepositoryPort = inviteTokenRepositoryPort;
        this.sessionRepositoryPort = sessionRepositoryPort;
        this.tokenHasher = tokenHasher;
    }

    /**
     * Валидирует пригласительный токен и создаёт/возвращает сессию сотрудника.
     *
     * <p>Операция read-modify-write (найти токен → создать сессию → пометить токен
     * использованным) выполняется в одной транзакции, чтобы исключить гонку при
     * одновременном открытии одной и той же пригласительной ссылки.
     *
     * @param token пригласительный токен
     * @return результат валидации или {@link Optional#empty()} если токен невалиден,
     *         истёк или уже использован
     */
    @Override
    @Transactional
    public Optional<InviteOutcome> validateInvite(String token) {
        String hash = tokenHasher.hash(token);
        return inviteTokenRepositoryPort.findByTokenHash(hash)
                .filter(InviteToken::isNotExpired)
                .filter(t -> !t.isUsed())
                .map(t -> {
                    // Первое использование — ищем существующую сессию сотрудника или создаём новую
                    Optional<AssessmentSession> existing = sessionRepositoryPort
                            .findFirstByEmployeeIdOrderByCreatedAtDesc(t.getEmployeeId());

                    AssessmentSession session;
                    boolean reused;
                    if (existing.isPresent()) {
                        session = existing.get();
                        reused = true;
                        logger.info("Переиспользована существующая сессия сотрудника: sessionId={}, employeeId={}",
                                session.getId(), t.getEmployeeId());
                    } else {
                        session = sessionRepositoryPort.save(
                                AssessmentSession.of(null, t.getEmployeeId(), null, null,
                                        SessionStatus.ACTIVE, null, null));
                        reused = false;
                        logger.info("Создана новая сессия оценки: sessionId={}, employeeId={}",
                                session.getId(), t.getEmployeeId());
                    }

                    inviteTokenRepositoryPort.save(t.markUsed(session.getId()));
                    return new InviteOutcome(session, reused);
                })
                .or(() -> {
                    logger.debug("Пригласительный токен невалиден, истёк или уже использован — доступ отклонён");
                    return Optional.empty();
                });
    }
}