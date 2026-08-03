package com.assessment.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HmacTokenValidator: генерация и валидация HMAC-токенов")
class HmacTokenValidatorTest {

    private HmacTokenValidator validator;

    private static final String SECRET = "test-secret-key";
    private static final String EMPLOYEE_ID = "550e8400-e29b-41d4-a716-446655440000";

    @BeforeEach
    void setUp() {
        validator = new HmacTokenValidator(SECRET);
    }

    @Test
    @DisplayName("Сгенерированный токен не null, не пуст, в формате Base64 URL-safe")
    void generateTokenReturnsNonNullBase64Url() {
        String token = validator.generateToken(EMPLOYEE_ID);
        assertNotNull(token);
        assertFalse(token.isEmpty());
        // Base64 URL-safe: нет + / =
        assertFalse(token.contains("+"));
        assertFalse(token.contains("/"));
        assertFalse(token.contains("="));
    }

    @Test
    @DisplayName("Валидный токен успешно проходит проверку")
    void validateTokenValidTokenReturnsTrue() {
        String token = validator.generateToken(EMPLOYEE_ID);
        assertTrue(validator.validateToken(EMPLOYEE_ID, token));
    }

    @Test
    @DisplayName("Неверный токен отклоняется")
    void validateTokenWrongTokenReturnsFalse() {
        String token = validator.generateToken(EMPLOYEE_ID);
        assertFalse(validator.validateToken(EMPLOYEE_ID, "some-other-token"));
    }

    @Test
    @DisplayName("Пустой токен отклоняется")
    void validateTokenEmptyTokenReturnsFalse() {
        assertFalse(validator.validateToken(EMPLOYEE_ID, ""));
    }

    @Test
    @DisplayName("Null-токен отклоняется")
    void validateTokenNullTokenReturnsFalse() {
        assertFalse(validator.validateToken(EMPLOYEE_ID, null));
    }

    @Test
    @DisplayName("Разные секреты дают разные токены")
    void differentSecretProducesDifferentToken() {
        HmacTokenValidator other = new HmacTokenValidator("different-secret");
        String t1 = validator.generateToken(EMPLOYEE_ID);
        String t2 = other.generateToken(EMPLOYEE_ID);
        assertNotEquals(t1, t2);
    }

    @Test
    @DisplayName("Детерминированность: один и тот же ID даёт одинаковый токен")
    void deterministicToken() {
        String t1 = validator.generateToken(EMPLOYEE_ID);
        String t2 = validator.generateToken(EMPLOYEE_ID);
        assertEquals(t1, t2);
    }

    @Test
    @DisplayName("Разные ID сотрудников дают разные токены")
    void differentEmployeeIdProducesDifferentToken() {
        String t1 = validator.generateToken("employee-1");
        String t2 = validator.generateToken("employee-2");
        assertNotEquals(t1, t2);
    }

    @Test
    @DisplayName("Пустой секрет вызывает ошибку при генерации")
    void emptySecretThrowsOnGenerate() {
        HmacTokenValidator v = new HmacTokenValidator("");
        // SecretKeySpec отклоняет пустой ключ IllegalArgumentException'ом
        assertThrows(IllegalArgumentException.class, () -> v.generateToken(EMPLOYEE_ID));
    }
}
