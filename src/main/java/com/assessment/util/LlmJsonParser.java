package com.assessment.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.RoundingMode;
import java.util.Optional;

/**
 * Парсер JSON-ответов LLM на базе Jackson.
 *
 * <p>Извлекает значения по ключу из JSON-ответа модели. В отличие от ручного
 * парсера корректно обрабатывает:
 * <ul>
 *   <li>экранированные кавычки в строковых значениях ({@code "He said \"hi\""});</li>
 *   <li>markdown-ограждения {@code ```json ... ```};</li>
 *   <li>точное совпадение ключей ({@code "score"} не путается с {@code "score_raw"});</li>
 *   <li>десятичные оценки ({@code "score": 4.5} округляется по HALF_UP).</li>
 * </ul>
 */
public final class LlmJsonParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LlmJsonParser() {
    }

    /**
     * Извлекает строковое значение по ключу из JSON-строки.
     *
     * @param json JSON-строка (допускаются markdown-ограждения ```json)
     * @param key  ключ для поиска
     * @return найденное значение или пустая строка, если ключ не найден или JSON невалиден
     */
    public static String extractJsonValue(String json, String key) {
        try {
            JsonNode root = MAPPER.readTree(stripCodeFences(json));
            if (root == null) {
                return "";
            }
            JsonNode node = root.get(key);
            if (node == null || node.isNull()) {
                return "";
            }
            return node.isValueNode() ? node.asText() : node.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Извлекает числовую оценку по ключу из JSON-строки с округлением десятичных значений.
     *
     * @param json JSON-строка (допускаются markdown-ограждения ```json)
     * @param key  ключ для поиска (обычно {@code "score"})
     * @return оценка, округлённая до целого (HALF_UP), или {@link Optional#empty()},
     *         если ключ отсутствует, значение нечисловое или JSON невалиден
     */
    public static Optional<Integer> extractScore(String json, String key) {
        try {
            JsonNode root = MAPPER.readTree(stripCodeFences(json));
            if (root == null) {
                return Optional.empty();
            }
            JsonNode node = root.get(key);
            if (node == null || !node.isNumber()) {
                return Optional.empty();
            }
            return Optional.of(node.decimalValue().setScale(0, RoundingMode.HALF_UP).intValue());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Убирает markdown-ограждения кода ({@code ```json ... ```}), если они присутствуют.
     *
     * @param json исходная строка ответа LLM
     * @return строка без ограждений (или исходная строка, если ограждений нет)
     */
    private static String stripCodeFences(String json) {
        if (json == null) {
            return "";
        }
        String trimmed = json.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstNewline = trimmed.indexOf('\n');
        int lastFence = trimmed.lastIndexOf("```");
        if (firstNewline == -1 || lastFence <= firstNewline) {
            return trimmed;
        }
        return trimmed.substring(firstNewline + 1, lastFence).trim();
    }
}
