package com.assessment.assessment.domain;

/**
 * Статус сессии оценки.
 */
public enum SessionStatus {

    /** Сессия активна — сотрудник отвечает на вопросы. */
    ACTIVE("ACTIVE"),

    /** Сессия завершена — все темы пройдены, доступен отчёт. */
    COMPLETED("COMPLETED");

    private final String value;

    SessionStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    /**
     * Возвращает статус по строковому значению из БД.
     *
     * @param value строковое значение статуса (например, из JPA-сущности)
     * @return соответствующий статус или {@code ACTIVE} для неизвестного значения
     */
    public static SessionStatus fromValue(String value) {
        for (SessionStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return ACTIVE;
    }
}
