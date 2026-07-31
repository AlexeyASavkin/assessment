package com.assessment.ai.domain;

/**
 * Шаблон промпта с placeholder'ами {@code %1$s}, {@code %2$s}, ...
 *
 * <p>Неизменяемый доменный объект: инкапсулирует текст шаблона (дефолтный
 * из кода или переопределённый в БД) и подстановку аргументов через
 * {@link String#format(String, Object...)}.
 */
public final class PromptTemplate {

    private final String template;

    public PromptTemplate(String template) {
        this.template = template;
    }

    /**
     * Подставляет аргументы в шаблон.
     *
     * @param args аргументы для placeholder'ов в порядке нумерации
     * @return готовый текст промпта
     */
    public String format(Object... args) {
        return String.format(template, args);
    }

    /**
     * @return исходный текст шаблона без подстановки
     */
    public String raw() {
        return template;
    }
}
