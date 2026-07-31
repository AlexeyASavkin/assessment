package com.assessment.assessment.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Доменная модель одноразового пригласительного токена.
 *
 * <p>Неизменяемая, не содержит JPA/Spring-зависимостей. Несёт данные токена,
 * привязанного к сотруднику и (после первого открытия) к сессии оценки.
 */
public final class InviteToken {

    private final UUID id;
    private final String tokenHash;
    private final UUID employeeId;
    private final UUID sessionId;
    private final boolean used;
    private final Instant usedAt;
    private final Instant expiresAt;
    private final Instant createdAt;

    private InviteToken(UUID id, String tokenHash, UUID employeeId, UUID sessionId, boolean used,
                        Instant usedAt, Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.tokenHash = tokenHash;
        this.employeeId = employeeId;
        this.sessionId = sessionId;
        this.used = used;
        this.usedAt = usedAt;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public static InviteToken of(UUID id, String tokenHash, UUID employeeId, UUID sessionId, boolean used,
                                 Instant usedAt, Instant expiresAt, Instant createdAt) {
        return new InviteToken(id, tokenHash, employeeId, sessionId, used, usedAt, expiresAt, createdAt);
    }

    /**
     * Помечает токен как использованный и привязывает его к сессии.
     *
     * @param newSessionId идентификатор сессии
     * @return новая модель токена с флагом {@code used = true}
     */
    public InviteToken markUsed(UUID newSessionId) {
        return new InviteToken(id, tokenHash, employeeId, newSessionId, true, Instant.now(), expiresAt, createdAt);
    }

    /**
     * @return {@code true}, если токен ещё не истёк
     */
    public boolean isNotExpired() {
        return expiresAt.isAfter(Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public boolean isUsed() {
        return used;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
