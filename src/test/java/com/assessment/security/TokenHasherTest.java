package com.assessment.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TokenHasher: SHA-256 хеширование токенов")
class TokenHasherTest {

    private final TokenHasher hasher = new TokenHasher();

    @Test
    @DisplayName("Хеш детерминирован: одинаковый вход даёт одинаковый хеш")
    void hashIsDeterministic() {
        String token = "some-random-token";
        assertEquals(hasher.hash(token), hasher.hash(token));
    }

    @Test
    @DisplayName("Хеш не содержит символов padding и Base64-спецсимволов")
    void hashHasNoPadding() {
        String hash = hasher.hash("some-random-token");
        assertFalse(hash.contains("="));
        assertFalse(hash.contains("+"));
        assertFalse(hash.contains("/"));
    }

    @Test
    @DisplayName("Разные входы дают разные хеши")
    void differentInputsProduceDifferentHashes() {
        assertNotEquals(hasher.hash("token-1"), hasher.hash("token-2"));
    }

    @Test
    @DisplayName("Пустая строка хешируется без исключений")
    void emptyStringHashesWithoutException() {
        String hash = hasher.hash("");
        assertNotNull(hash);
        assertFalse(hash.isEmpty());
    }

    @Test
    @DisplayName("Хеш 32-байтного токена имеет длину 43 символа")
    void hashOfRandomTokenHasLength43() {
        String token = new RandomTokenGenerator().generate();
        assertEquals(43, hasher.hash(token).length());
    }
}