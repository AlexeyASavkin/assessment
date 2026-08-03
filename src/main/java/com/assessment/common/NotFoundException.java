package com.assessment.common;

/**
 * Доменное исключение: запрошенный ресурс не найден.
 * <p>
 * Маппится {@code @RestControllerAdvice} на HTTP 404.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}