package com.assessment.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Fail-fast валидатор секретов prod-профиля.
 * <p>
 * Активируется ТОЛЬКО на профиле {@code prod} ({@code @Profile("prod")}):
 * dev/BDD-профили (default) работают с тестовыми секретами и не затрагиваются.
 * <p>
 * Читает те же переменные окружения, что и приложение:
 * <ul>
 *   <li>{@code ADMIN_USERNAME} / {@code ADMIN_PASSWORD_HASH} — напрямую из окружения
 *       (как {@code AdminUserDetailsService});</li>
 *   <li>{@code HMAC_SECRET} — через свойство {@code assessment.security.hmac-secret}
 *       из {@code application.yml} (как {@code HmacTokenValidator}).</li>
 * </ul>
 * Если какой-либо секрет равен известному небезопасному значению по умолчанию
 * из {@code .env.example}, приложение НЕ стартует: выбрасывается
 * {@link IllegalStateException} на этапе {@link PostConstruct}.
 */
@Configuration
@Profile("prod")
public class SecretsValidator {

    private static final Logger log = LoggerFactory.getLogger(SecretsValidator.class);

    /** Значение HMAC_SECRET по умолчанию из .env.example (небезопасно для prod). */
    static final String DEFAULT_HMAC_SECRET = "change-me-in-production";

    /** BCrypt-хэш пароля «admin» из .env.example (небезопасен для prod). */
    static final String DEFAULT_ADMIN_PASSWORD_HASH =
            "{bcrypt}$2a$12$3bgslYyY8l4jFEUHSNTsxe53SzKCU93CuHf.fFF4MaVTlK1RBATR2";

    /** Та же строка в «экранированном» виде ($$) — как она выглядит в .env.example до разэкранирования. */
    static final String DEFAULT_ADMIN_PASSWORD_HASH_ESCAPED =
            "{bcrypt}$$2a$$12$$3bgslYyY8l4jFEUHSNTsxe53SzKCU93CuHf.fFF4MaVTlK1RBATR2";

    private final String adminUsername;
    private final String adminPasswordHash;
    private final String hmacSecret;

    /**
     * Конструктор с теми же биндингами, что использует приложение:
     * {@code ADMIN_USERNAME} и {@code ADMIN_PASSWORD_HASH} — переменные окружения,
     * {@code hmacSecret} — свойство {@code assessment.security.hmac-secret}
     * (в {@code application.yml} = {@code ${HMAC_SECRET}}).
     */
    public SecretsValidator(
            @Value("${ADMIN_USERNAME:}") String adminUsername,
            @Value("${ADMIN_PASSWORD_HASH:}") String adminPasswordHash,
            @Value("${assessment.security.hmac-secret:}") String hmacSecret) {
        this.adminUsername = adminUsername;
        this.adminPasswordHash = adminPasswordHash;
        this.hmacSecret = hmacSecret;
    }

    /**
     * Проверяет секреты при старте приложения на профиле {@code prod}.
     * При обнаружении небезопасного значения выбрасывает
     * {@link IllegalStateException} — приложение не стартует (fail-fast).
     */
    @PostConstruct
    public void validate() {
        if (adminUsername == null || adminUsername.isBlank()) {
            log.error("Секрет не настроен: переменная окружения ADMIN_USERNAME пуста или не задана. "
                    + "Задайте ADMIN_USERNAME перед запуском prod-профиля.");
            throw new IllegalStateException(
                    "Секрет не настроен: ADMIN_USERNAME пуст или не задан. "
                            + "Задайте переменную окружения ADMIN_USERNAME перед запуском prod-профиля.");
        }
        if (DEFAULT_ADMIN_PASSWORD_HASH.equals(adminPasswordHash)
                || DEFAULT_ADMIN_PASSWORD_HASH_ESCAPED.equals(adminPasswordHash)) {
            log.error("Секрет небезопасен: ADMIN_PASSWORD_HASH равен значению по умолчанию из .env.example "
                    + "(BCrypt-хэш пароля «admin»). Задайте собственный BCrypt-хэш пароля администратора.");
            throw new IllegalStateException(
                    "Секрет небезопасен: ADMIN_PASSWORD_HASH равен значению по умолчанию из .env.example "
                            + "(BCrypt-хэш пароля «admin»). Задайте собственный BCrypt-хэш пароля администратора.");
        }
        if (DEFAULT_HMAC_SECRET.equals(hmacSecret)) {
            log.error("Секрет небезопасен: HMAC_SECRET равен значению по умолчанию «change-me-in-production» "
                    + "из .env.example. Задайте собственный HMAC-секрет.");
            throw new IllegalStateException(
                    "Секрет небезопасен: HMAC_SECRET равен значению по умолчанию «change-me-in-production» "
                            + "из .env.example. Задайте собственный HMAC-секрет.");
        }
    }
}