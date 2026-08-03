package com.assessment.common;

/**
 * Доменное исключение: LLM-провайдер недоступен (таймаут, 429/5xx, network).
 * <p>
 * Маппится {@code @RestControllerAdvice} на HTTP 503 Service Unavailable.
 */
public class LlmUnavailableException extends RuntimeException {
    public LlmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}