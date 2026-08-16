package com.assessment.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Генератор случайных 256-битных пригласительных токенов.
 * <p>
 * Токен — 32 случайных байта из {@link SecureRandom}, закодированные в
 * Base64 URL-safe без padding (43 символа, алфавит {@code [A-Za-z0-9_-]}).
 * В отличие от детерминированных HMAC-подписей, случайный токен невозможно
 * предсказать по идентификатору сотрудника.
 */
@Component
public class RandomTokenGenerator {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Генерирует новый случайный токен.
     *
     * @return строка из 43 символов Base64 URL-safe без padding
     */
    public String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}