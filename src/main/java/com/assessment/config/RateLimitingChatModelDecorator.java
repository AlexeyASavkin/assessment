package com.assessment.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Декоратор, оборачивающий вызовы ChatModel с Rate Limiting через Resilience4j.
 * <p>
 * При превышении лимита выбросывает {@code RequestNotPermitted} из Resilience4j.
 */
public class RateLimitingChatModelDecorator implements ChatModel {

    private final ChatModel delegate;
    private final RateLimiter rateLimiter;

    public RateLimitingChatModelDecorator(ChatModel delegate, RateLimiterRegistry registry, String rateLimiterName) {
        this.delegate = delegate;
        this.rateLimiter = registry.rateLimiter(rateLimiterName);
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        return RateLimiter.decorateSupplier(rateLimiter, () -> delegate.call(prompt)).get();
    }
}
