package com.assessment.common;

/**
 * Доменное исключение: некорректный запрос (невалидный аргумент).
 * <p>
 * Маппится {@code @RestControllerAdvice} на HTTP 400.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}