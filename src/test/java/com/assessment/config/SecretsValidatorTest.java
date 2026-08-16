package com.assessment.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Юнит-тесты валидатора секретов prod-профиля.
 * <p>
 * Чистый unit-тест: значения секретов передаются напрямую в конструктор,
 * окружение не трогается — тест детерминирован.
 */
@DisplayName("SecretsValidator: fail-fast проверка секретов prod-профиля")
class SecretsValidatorTest {

    private static final String STRONG_HMAC =
            "2f9d8c7b6a5e4d3c2b1a0987654321fedcba9876543210fedcba9876543210ab";
    private static final String STRONG_HASH = "$2a$10$SomeStrongHashThatIsNotTheDefault";
    private static final String DEFAULT_HMAC = "change-me-in-production";
    private static final String DEFAULT_HASH =
            "{bcrypt}$2a$12$3bgslYyY8l4jFEUHSNTsxe53SzKCU93CuHf.fFF4MaVTlK1RBATR2";
    private static final String DEFAULT_HASH_ESCAPED =
            "{bcrypt}$$2a$$12$$3bgslYyY8l4jFEUHSNTsxe53SzKCU93CuHf.fFF4MaVTlK1RBATR2";

    @Test
    @DisplayName("Пропускает сильные секреты без исключений")
    void acceptsStrongSecrets() {
        SecretsValidator validator = new SecretsValidator("admin", STRONG_HASH, STRONG_HMAC);

        assertDoesNotThrow(validator::validate);
    }

    @Test
    @DisplayName("Отклоняет HMAC_SECRET по умолчанию из .env.example")
    void rejectsDefaultHmacSecret() {
        SecretsValidator validator = new SecretsValidator("admin", STRONG_HASH, DEFAULT_HMAC);

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("HMAC_SECRET"),
                "Сообщение должно называть проблемный секрет, но было: " + ex.getMessage());
    }

    @Test
    @DisplayName("Отклоняет ADMIN_PASSWORD_HASH по умолчанию из .env.example")
    void rejectsDefaultAdminPasswordHash() {
        SecretsValidator validator = new SecretsValidator("admin", DEFAULT_HASH, STRONG_HMAC);

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("ADMIN_PASSWORD_HASH"),
                "Сообщение должно называть проблемный секрет, но было: " + ex.getMessage());
    }

    @Test
    @DisplayName("Отклоняет ADMIN_PASSWORD_HASH с экранированными $$ из .env.example")
    void rejectsEscapedDefaultAdminPasswordHash() {
        SecretsValidator validator = new SecretsValidator("admin", DEFAULT_HASH_ESCAPED, STRONG_HMAC);

        assertThrows(IllegalStateException.class, validator::validate);
    }

    @Test
    @DisplayName("Отклоняет пустой ADMIN_USERNAME")
    void rejectsBlankAdminUsername() {
        SecretsValidator validator = new SecretsValidator("", STRONG_HASH, STRONG_HMAC);

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("ADMIN_USERNAME"),
                "Сообщение должно называть проблемный секрет, но было: " + ex.getMessage());
    }

    @Test
    @DisplayName("Отклоняет null ADMIN_USERNAME без NPE")
    void rejectsNullAdminUsername() {
        SecretsValidator validator = new SecretsValidator(null, STRONG_HASH, STRONG_HMAC);

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertTrue(ex.getMessage().contains("ADMIN_USERNAME"),
                "Сообщение должно называть проблемный секрет, но было: " + ex.getMessage());
    }
}