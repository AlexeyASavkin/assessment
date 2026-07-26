package com.assessment.util;

/**
 * Минималистичный парсер JSON-ответов LLM без зависимости от полноценного JSON-парсера.
 * Используется сервисами скоринга/переоценки для извлечения значений по ключу.
 *
 * <p>Поддерживает:
 * <ul>
 *   <li>строковые значения в кавычках — {@code "key": "value"}</li>
 *   <li>числовые/булевы без кавычек — {@code "key": 5}</li>
 * </ul>
 */
public final class LlmJsonParser {

    private LlmJsonParser() {
    }

    /**
     * Извлекает строковое значение по ключу из JSON-строки.
     *
     * @param json JSON-строка
     * @param key  ключ для поиска
     * @return найденное значение или пустая строка, если ключ не найден
     */
    public static String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return "";

        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return "";

        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && json.charAt(valueStart) == ' ') {
            valueStart++;
        }
        if (valueStart >= json.length()) return "";

        if (json.charAt(valueStart) == '"') {
            // Строковое значение в кавычках
            valueStart++;
            int valueEnd = json.indexOf('"', valueStart);
            if (valueEnd == -1) return "";
            return json.substring(valueStart, valueEnd);
        } else {
            // Числовое / булево значение без кавычек — до запятой или закрывающей скобки
            int valueEnd = valueStart;
            while (valueEnd < json.length() && json.charAt(valueEnd) != ',' && json.charAt(valueEnd) != '}') {
                valueEnd++;
            }
            return json.substring(valueStart, valueEnd).trim();
        }
    }
}