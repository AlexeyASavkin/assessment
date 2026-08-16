package com.assessment.config;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("LoginAttemptRateLimiter: блокировка входа после 5 неудачных попыток")
class LoginAttemptRateLimiterTest {

    private LoginAttemptRateLimiter limiter;

    @BeforeEach
    void setUp() {
        RateLimiterRegistry registry = RateLimiterRegistry.ofDefaults();
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(5)
                .limitRefreshPeriod(Duration.ofMillis(500))
                .timeoutDuration(Duration.ZERO)
                .build();
        limiter = new LoginAttemptRateLimiter(registry, config);
    }

    @Test
    @DisplayName("Пропускает первые 5 неудачных попыток, 6-ю отклоняет")
    void allowsFiveAttemptsThenRejects() {
        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> limiter.check("admin"));
        }

        assertThrows(RequestNotPermitted.class, () -> limiter.check("admin"));
    }

    @Test
    @DisplayName("Разные пользователи получают независимые счётчики попыток")
    void usernamesHaveIndependentBuckets() {
        for (int i = 0; i < 5; i++) {
            limiter.check("admin");
        }
        assertThrows(RequestNotPermitted.class, () -> limiter.check("admin"));

        assertDoesNotThrow(() -> limiter.check("other"));
    }

    @Test
    @DisplayName("Окно блокировки сбрасывается после истечения периода")
    void windowResetsAfterRefreshPeriod() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            limiter.check("admin");
        }
        assertThrows(RequestNotPermitted.class, () -> limiter.check("admin"));

        Thread.sleep(700);
        assertDoesNotThrow(() -> limiter.check("admin"));
    }

    @Test
    @DisplayName("isBlocked отражает состояние bucket без потребления разрешений")
    void isBlockedReflectsBucketState() throws InterruptedException {
        assertFalse(limiter.isBlocked("admin"));

        for (int i = 0; i < 5; i++) {
            limiter.check("admin");
        }
        assertTrue(limiter.isBlocked("admin"));

        Thread.sleep(700);
        assertFalse(limiter.isBlocked("admin"));
    }
}