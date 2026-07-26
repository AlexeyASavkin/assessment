package com.assessment.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Конфигурация Resilience4j для ограничения скорости вызовов внешних API.
 * <p>
 * Защищает приложение от превышения лимитов LLM-провайдеров,
 * настраивая rate limiter с лимитом 15 запросов в минуту.
 */
@Configuration
public class Resilience4jConfig {

    /**
     * Создает реестр rate limiter'ов с настройками для Gemini API.
     * <p>
     * Лимит: 15 запросов за период обновления 1 минута,
     * таймаут ожидания разрешения: 10 секунд.
     *
     * @return реестр {@link RateLimiterRegistry} с преднастроенным конфигом
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(15)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofSeconds(10))
                .build();

        return RateLimiterRegistry.of(config);
    }
}
