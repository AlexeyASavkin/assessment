package com.assessment.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Хеширует пригласительные токены для хранения в БД.
 * <p>
 * SHA-256 хеш токена кодируется в Base64 URL-safe без padding (43 символа).
 * Хеширование детерминированное: одинаковый вход всегда даёт одинаковый хеш,
 * поэтому по хешу в БД можно найти токен, не храня его в открытом виде.
 */
@Component
public class TokenHasher {

    /**
     * Вычисляет SHA-256 хеш токена в Base64 URL-safe без padding.
     *
     * @param token исходный токен
     * @return детерминированный хеш токена
     */
    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 гарантированно доступен в любой JVM
            throw new IllegalStateException("SHA-256 недоступен", e);
        }
    }
}