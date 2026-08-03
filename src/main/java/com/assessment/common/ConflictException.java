package com.assessment.common;

/**
 * Доменное исключение: нарушение уникальности или конфликт состояния.
 * <p>
 * Маппится {@code @RestControllerAdvice} на HTTP 409.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}