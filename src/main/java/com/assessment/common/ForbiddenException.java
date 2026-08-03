package com.assessment.common;

/**
 * Доменное исключение: доступ к чужому ресурсу запрещён.
 * <p>
 * Маппится {@code @RestControllerAdvice} на HTTP 403.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}