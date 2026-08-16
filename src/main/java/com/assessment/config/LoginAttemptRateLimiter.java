package com.assessment.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Персональный rate limiter неудачных попыток входа администратора.
 *
 * <p>Каждый username получает собственный bucket (имя лимитера
 * {@code login:<username>}), поэтому блокировка одного пользователя не
 * затрагивает остальных: 5 неудачных попыток за 15 минут исчерпывают bucket,
 * после чего {@code acquirePermission()} возвращает {@code false} и лимитер
 * выбрасывает {@code RequestNotPermitted} (timeoutDuration = 0 — отказ без
 * ожидания). Вызов выполняется напрямую из failure handler'а form-login
 * ({@link SecurityConfig}), который сам пишет HTTP 429 + {@code Retry-After},
 * минуя {@link GlobalExceptionHandler} (тот обрабатывает только исключения
 * контроллеров, а не фильтров безопасности).
 */
@Component
public class LoginAttemptRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(LoginAttemptRateLimiter.class);

    /** Максимум неудачных попыток входа до блокировки. */
    static final int MAX_FAILED_ATTEMPTS = 5;

    /** Окно блокировки: 15 минут. */
    static final Duration LOCKOUT_WINDOW = Duration.ofMinutes(15);

    private final RateLimiterRegistry rateLimiterRegistry;
    private final RateLimiterConfig limiterConfig;

    /**
     * Конструктор с конфигом по умолчанию: 5 попыток / 15 минут, отказ без ожидания.
     *
     * @param rateLimiterRegistry реестр rate limiter'ов Resilience4j
     */
    @Autowired
    public LoginAttemptRateLimiter(RateLimiterRegistry rateLimiterRegistry) {
        this(rateLimiterRegistry, defaultConfig());
    }

    /**
     * Конструктор с произвольным конфигом (используется в unit-тестах
     * для проверки сброса окна на коротком периоде).
     *
     * @param rateLimiterRegistry реестр rate limiter'ов Resilience4j
     * @param limiterConfig       конфиг bucket'а попыток входа
     */
    LoginAttemptRateLimiter(RateLimiterRegistry rateLimiterRegistry, RateLimiterConfig limiterConfig) {
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.limiterConfig = limiterConfig;
    }

    private static RateLimiterConfig defaultConfig() {
        return RateLimiterConfig.custom()
                .limitForPeriod(MAX_FAILED_ATTEMPTS)
                .limitRefreshPeriod(LOCKOUT_WINDOW)
                .timeoutDuration(Duration.ZERO)
                .build();
    }

    /**
     * Регистрирует неудачную попытку входа и проверяет лимит пользователя.
     *
     * @param username имя пользователя из form-параметра {@code username}
     * @throws RequestNotPermitted если лимит неудачных попыток исчерпан
     */
    public void check(String username) {
        String key = (username == null || username.isBlank()) ? "unknown" : username;
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("login:" + key, limiterConfig);
        if (!rateLimiter.acquirePermission()) {
            logger.warn("Превышен лимит неудачных попыток входа: username={}, bucket={}", key, rateLimiter.getName());
            throw RequestNotPermitted.createRequestNotPermitted(rateLimiter);
        }
    }

    /**
     * Проверяет, исчерпан ли лимит неудачных попыток пользователя, без потребления
     * разрешения. Используется фильтром блокировки входа ДО аутентификации, чтобы
     * отклонять любые попытки входа (включая с верным паролем), пока окно
     * блокировки активно. {@code getAvailablePermissions()} учитывает ленивое
     * пополнение bucket'а по истёкшему периоду, поэтому после 15 минут блокировка
     * снимается автоматически.
     *
     * @param username имя пользователя из form-параметра {@code username}
     * @return true, если bucket исчерпан и вход заблокирован
     */
    public boolean isBlocked(String username) {
        String key = (username == null || username.isBlank()) ? "unknown" : username;
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("login:" + key, limiterConfig);
        return rateLimiter.getMetrics().getAvailablePermissions() <= 0;
    }
}