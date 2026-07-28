package com.assessment.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LlmJsonParser: извлечение значений из JSON")
class LlmJsonParserTest {

    @Test
    @DisplayName("Извлечение строкового значения")
    void extractStringValue() {
        String json = "{\"score\": 4, \"confidence\": \"HIGH\", \"feedback\": \"Good job\"}";
        assertEquals("HIGH", LlmJsonParser.extractJsonValue(json, "confidence"));
    }

    @Test
    @DisplayName("Извлечение числового значения")
    void extractNumericValue() {
        String json = "{\"score\": 4, \"confidence\": \"HIGH\"}";
        assertEquals("4", LlmJsonParser.extractJsonValue(json, "score"));
    }

    @Test
    @DisplayName("Извлечение поля feedback")
    void extractFeedback() {
        String json = "{\"score\": 2, \"feedback\": \"Нужно подучить теорию\"}";
        assertEquals("Нужно подучить теорию", LlmJsonParser.extractJsonValue(json, "feedback"));
    }

    @Test
    @DisplayName("Отсутствующий ключ возвращает пустую строку")
    void keyNotFoundReturnsEmpty() {
        String json = "{\"score\": 5}";
        assertEquals("", LlmJsonParser.extractJsonValue(json, "missing"));
    }

    @Test
    @DisplayName("Пустой JSON возвращает пустую строку")
    void emptyJsonReturnsEmpty() {
        assertEquals("", LlmJsonParser.extractJsonValue("{}", "key"));
    }

    @Test
    @DisplayName("Ключ без двоеточия возвращает пустую строку")
    void noColonAfterKeyReturnsEmpty() {
        assertEquals("", LlmJsonParser.extractJsonValue("{\"key\" 123}", "key"));
    }

    @Test
    @DisplayName("Значение с пробелами сохраняется")
    void valueWithSpaces() {
        String json = "{\"feedback\": \"  needs improvement  \"}";
        assertEquals("  needs improvement  ", LlmJsonParser.extractJsonValue(json, "feedback"));
    }

    @Test
    @DisplayName("Числовое значение без кавычек (boolean)")
    void numericValueWithoutQuotes() {
        String json = "{\"valid_judge\": true}";
        assertEquals("true", LlmJsonParser.extractJsonValue(json, "valid_judge"));
    }

    @Test
    @DisplayName("Извлечение значения до запятой")
    void valueUntilComma() {
        String json = "{\"a\": 1, \"b\": 2}";
        assertEquals("1", LlmJsonParser.extractJsonValue(json, "a"));
    }

    @Test
    @DisplayName("Извлечение значения до закрывающей скобки")
    void valueUntilClosingBrace() {
        String json = "{\"a\": 42}";
        assertEquals("42", LlmJsonParser.extractJsonValue(json, "a"));
    }

    @Test
    @DisplayName("Значение сразу после двоеточия без пробелов")
    void valueNextToColonWithoutSpaces() {
        String json = "{\"a\":\"b\"}";
        assertEquals("b", LlmJsonParser.extractJsonValue(json, "a"));
    }

    @Test
    @DisplayName("Многострочный JSON")
    void multilineJson() {
        String json = """
                {
                    "score": 3,
                    "confidence": "MEDIUM"
                }""";
        assertEquals("3", LlmJsonParser.extractJsonValue(json, "score"));
        assertEquals("MEDIUM", LlmJsonParser.extractJsonValue(json, "confidence"));
    }

    @ParameterizedTest
    @DisplayName("Отрицательные и граничные значения")
    @CsvSource({
        "\"score\": 0}, 0",
        "\"score\": -1}, -1",
        "\"score\": 5}, 5"
    })
    void negativeAndEdgeCases(String input, String expected) {
        String json = "{" + input;
        assertEquals(expected, LlmJsonParser.extractJsonValue(json, "score"));
    }

    @Test
    @DisplayName("Похожий, но не точный ключ — берётся первое совпадение")
    void keySimilarButNotExact() {
        String json = "{\"score\": 4, \"score_raw\": 5}";
        // Поиск по "score" найдёт первое вхождение — 4
        assertEquals("4", LlmJsonParser.extractJsonValue(json, "score"));
    }

    @Test
    @DisplayName("Приватный конструктор недоступен извне")
    void privateConstructorCannotBeAccessed() {
        var constructor = LlmJsonParser.class.getDeclaredConstructors()[0];
        assertFalse(constructor.canAccess(null));
    }
}
