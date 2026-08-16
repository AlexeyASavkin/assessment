package com.assessment.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RandomTokenGenerator: генерация случайных 256-битных токенов")
class RandomTokenGeneratorTest {

    private static final Pattern BASE64URL_CHARSET = Pattern.compile("^[A-Za-z0-9_-]+$");

    private final RandomTokenGenerator generator = new RandomTokenGenerator();

    @Test
    @DisplayName("Сгенерированный токен имеет длину 43 символа (32 байта в Base64URL без padding)")
    void generatedTokenHasLength43() {
        String token = generator.generate();
        assertEquals(43, token.length());
    }

    @Test
    @DisplayName("Сгенерированный токен состоит только из символов Base64URL без padding")
    void generatedTokenMatchesBase64UrlCharset() {
        String token = generator.generate();
        assertTrue(BASE64URL_CHARSET.matcher(token).matches());
        assertFalse(token.contains("="));
    }

    @Test
    @DisplayName("1000 сгенерированных токенов уникальны")
    void thousandGeneratedTokensAreUnique() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            tokens.add(generator.generate());
        }
        assertEquals(1000, tokens.size());
    }

    @Test
    @DisplayName("Последовательные генерации не повторяются")
    void consecutiveGenerationsDiffer() {
        assertNotEquals(generator.generate(), generator.generate());
    }
}