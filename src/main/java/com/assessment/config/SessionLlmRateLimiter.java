package com.assessment.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Персональный rate limiter LLM-вызовов на сессию сотрудника.
 *
 * <p>Каждая сессия получает собственный bucket (имя лимитера
 * {@code session:<sessionId>}), поэтому один активный сотрудник не может
 * «старвить» остальных: 5 одновременных сессий со 4 ответами в минуту
 * суммарно съедают 20 запросов, но каждая сессия упирается только в свой
 * лимит. При превышении лимита Resilience4j выбрасывает
 * {@code RequestNotPermitted}, который {@link GlobalExceptionHandler}
 * превращает в HTTP 429 с заголовком {@code Retry-After}.
 */
@Component
public class SessionLlmRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(SessionLlmRateLimiter.class);

    private final RateLimiterRegistry rateLimiterRegistry;

    public SessionLlmRateLimiter(RateLimiterRegistry rateLimiterRegistry) {
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    /**
     * Выполняет LLM-вызов под rate limit'ом сессии.
     *
     * @param sessionId идентификатор сессии сотрудника
     * @param call      вызов LLM
     * @return результат вызова
     * @throws io.github.resilience4j.ratelimiter.RequestNotPermitted если лимит сессии исчерпан
     */
    public <T> T execute(UUID sessionId, Supplier<T> call) {
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("session:" + sessionId);
        logger.debug("LLM-вызов под rate limit'ом: sessionId={}, bucket={}", sessionId, rateLimiter.getName());
        try {
            return RateLimiter.decorateSupplier(rateLimiter, call).get();
        } catch (RequestNotPermitted e) {
            logger.warn("Лимит LLM-вызовов сессии исчерпан: sessionId={}, bucket={}", sessionId, rateLimiter.getName());
            throw e;
        }
    }
}
