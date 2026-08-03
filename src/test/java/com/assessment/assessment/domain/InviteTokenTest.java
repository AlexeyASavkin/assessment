package com.assessment.assessment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InviteToken: неизменяемая доменная модель пригласительного токена")
class InviteTokenTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID EMPLOYEE_ID = UUID.randomUUID();
    private static final Instant CREATED_AT = Instant.now();

    @Test
    @DisplayName("of сохраняет все поля и возвращает их через геттеры")
    void ofStoresAllFields() {
        UUID sessionId = UUID.randomUUID();
        Instant usedAt = Instant.now();
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.DAYS);

        InviteToken token = InviteToken.of(ID, "hash", EMPLOYEE_ID, sessionId, true, usedAt, expiresAt, CREATED_AT);

        assertEquals(ID, token.getId());
        assertEquals("hash", token.getTokenHash());
        assertEquals(EMPLOYEE_ID, token.getEmployeeId());
        assertEquals(sessionId, token.getSessionId());
        assertTrue(token.isUsed());
        assertEquals(usedAt, token.getUsedAt());
        assertEquals(expiresAt, token.getExpiresAt());
        assertEquals(CREATED_AT, token.getCreatedAt());
    }

    @Test
    @DisplayName("markUsed возвращает использованный токен с привязкой к сессии, оригинал не меняется")
    void markUsedReturnsUsedTokenAndKeepsOriginal() {
        InviteToken original = InviteToken.of(ID, "hash", EMPLOYEE_ID, null, false, null,
                Instant.now().plus(1, ChronoUnit.DAYS), CREATED_AT);
        UUID sessionId = UUID.randomUUID();

        InviteToken used = original.markUsed(sessionId);

        assertNotSame(original, used);
        assertFalse(original.isUsed());
        assertNull(original.getSessionId());
        assertNull(original.getUsedAt());
        assertTrue(used.isUsed());
        assertEquals(sessionId, used.getSessionId());
        assertNotNull(used.getUsedAt());
        assertEquals(original.getId(), used.getId());
        assertEquals(original.getTokenHash(), used.getTokenHash());
        assertEquals(original.getEmployeeId(), used.getEmployeeId());
        assertEquals(original.getExpiresAt(), used.getExpiresAt());
        assertEquals(original.getCreatedAt(), used.getCreatedAt());
    }

    @Test
    @DisplayName("isNotExpired возвращает true для токена со сроком действия в будущем")
    void isNotExpiredReturnsTrueForFutureExpiry() {
        InviteToken token = InviteToken.of(ID, "hash", EMPLOYEE_ID, null, false, null,
                Instant.now().plus(1, ChronoUnit.HOURS), CREATED_AT);

        assertTrue(token.isNotExpired());
    }

    @Test
    @DisplayName("isNotExpired возвращает false для истёкшего токена")
    void isNotExpiredReturnsFalseForPastExpiry() {
        InviteToken token = InviteToken.of(ID, "hash", EMPLOYEE_ID, null, false, null,
                Instant.now().minus(1, ChronoUnit.HOURS), CREATED_AT);

        assertFalse(token.isNotExpired());
    }
}
