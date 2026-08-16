package com.assessment.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Компонент для генерации и проверки HMAC SHA-256 токенов.
 * <p>
 * Используется для создания детерминированных подписей пригласительных ссылок
 * сотрудников и cookie сессий. Секретный ключ задается через настройку
 * {@code assessment.security.hmac-secret}.
 */
@Component
public class HmacTokenValidator {

    private static final Logger logger = LoggerFactory.getLogger(HmacTokenValidator.class);

    private final String secret;

    /**
     * Конструктор, принимающий секретный ключ для HMAC.
     *
     * @param secret секретная строка для подписи токенов
     */
    public HmacTokenValidator(@Value("${assessment.security.hmac-secret}") String secret) {
        this.secret = secret;
    }

    /**
     * Генерирует HMAC SHA-256 токен для заданного идентификатора.
     * <p>
     * Результат кодируется в Base64 URL-safe без padding.
     *
     * @param employeeId идентификатор сотрудника или сессии
     * @return строковое представление HMAC-подписи
     * @throws RuntimeException при ошибке криптографического алгоритма
     */
    public String generateToken(String employeeId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(employeeId.getBytes(StandardCharsets.UTF_8));
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            logger.trace("Сгенерирован HMAC-токен: employeeId={}, длина токена={}", employeeId, token.length());
            return token;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate HMAC token", e);
        }
    }

    /**
     * Проверяет соответствие токена ожидаемой HMAC-подписи для идентификатора.
     * <p>
     * Сравнение выполняется через {@link MessageDigest#isEqual} — константное
     * по времени, не подверженное timing-атакам.
     *
     * @param employeeId идентификатор сотрудника или сессии
     * @param token предоставленный токен для проверки
     * @return true, если токен валиден; false при несовпадении или ошибке
     */
    public boolean validateToken(String employeeId, String token) {
        try {
            String expected = generateToken(employeeId);
            boolean valid = MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    token.getBytes(StandardCharsets.UTF_8));
            if (valid) {
                logger.debug("HMAC-токен валиден: employeeId={}", employeeId);
            } else {
                logger.warn("HMAC-токен не совпадает: employeeId={}", employeeId);
            }
            return valid;
        } catch (Exception e) {
            return false;
        }
    }
}
