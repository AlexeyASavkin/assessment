package com.assessment.config;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SessionLlmRateLimiter: персональные bucket'и rate limit на сессию")
class SessionLlmRateLimiterTest {

    private SessionLlmRateLimiter limiter;

    @BeforeEach
    void setUp() {
        RateLimiterRegistry registry = RateLimiterRegistry.of(RateLimiterConfig.custom()
                .limitForPeriod(2)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofMillis(100))
                .build());
        limiter = new SessionLlmRateLimiter(registry);
    }

    @Test
    @DisplayName("Пропускает вызовы в пределах лимита сессии")
    void allowsCallsWithinLimit() {
        UUID sessionId = UUID.randomUUID();

        assertEquals("ok", limiter.execute(sessionId, () -> "ok"));
        assertEquals("ok", limiter.execute(sessionId, () -> "ok"));
    }

    @Test
    @DisplayName("Выбрасывает RequestNotPermitted при превышении лимита сессии")
    void rejectsCallsBeyondLimit() {
        UUID sessionId = UUID.randomUUID();
        limiter.execute(sessionId, () -> "ok");
        limiter.execute(sessionId, () -> "ok");

        assertThrows(RequestNotPermitted.class, () -> limiter.execute(sessionId, () -> "ok"));
    }

    @Test
    @DisplayName("Разные сессии получают независимые лимиты")
    void sessionsHaveIndependentLimits() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        limiter.execute(first, () -> "ok");
        limiter.execute(first, () -> "ok");
        assertThrows(RequestNotPermitted.class, () -> limiter.execute(first, () -> "ok"));

        assertEquals("ok", limiter.execute(second, () -> "ok"));
        assertEquals("ok", limiter.execute(second, () -> "ok"));
    }
}
