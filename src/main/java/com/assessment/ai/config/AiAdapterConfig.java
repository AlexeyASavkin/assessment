package com.assessment.ai.config;

import com.assessment.ai.adapter.SpringAiFollowUpAdapter;
import com.assessment.ai.adapter.SpringAiQuestionGenerationAdapter;
import com.assessment.ai.adapter.SpringAiScoringAdapter;
import com.assessment.ai.adapter.StubFollowUpAdapter;
import com.assessment.ai.adapter.StubQuestionGenerationAdapter;
import com.assessment.ai.adapter.StubScoringAdapter;
import com.assessment.service.AiProviderService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация LLM-адаптеров выходных портов AI bounded context'а.
 *
 * <p>Регистрирует реализации портов {@code LlmScoringPort},
 * {@code LlmQuestionGenerationPort} и {@code LlmFollowUpPort} в зависимости
 * от активного AI-провайдера ({@code assessment.ai.active-provider},
 * env {@code AI_PROVIDER}, по умолчанию {@code gemini}).
 *
 * <p>Взаимное исключение реализаций:
 * <ul>
 *   <li>{@code stub} — регистрируются только stub-адаптеры с детерминированными
 *       ответами (для BDD-тестов без реального LLM);</li>
 *   <li>любое другое значение ({@code gemini}, {@code gigachat}, {@code openrouter},
 *       {@code opencode}) — регистрируются только Spring AI-адаптеры поверх
 *       {@link ChatModel}.</li>
 * </ul>
 * Условия {@link ConditionalOnProperty} и {@link ConditionalOnExpression}
 * взаимно исключают друг друга, поэтому для каждого порта в контексте
 * всегда присутствует ровно один бин-реализация.
 */
@Configuration
public class AiAdapterConfig {

    // --- Stub-адаптеры: только при assessment.ai.active-provider=stub ---

    @Bean
    @ConditionalOnProperty(name = "assessment.ai.active-provider", havingValue = "stub")
    public StubScoringAdapter stubScoringAdapter() {
        return new StubScoringAdapter();
    }

    @Bean
    @ConditionalOnProperty(name = "assessment.ai.active-provider", havingValue = "stub")
    public StubQuestionGenerationAdapter stubQuestionGenerationAdapter() {
        return new StubQuestionGenerationAdapter();
    }

    @Bean
    @ConditionalOnProperty(name = "assessment.ai.active-provider", havingValue = "stub")
    public StubFollowUpAdapter stubFollowUpAdapter() {
        return new StubFollowUpAdapter();
    }

    // --- Spring AI-адаптеры: для всех провайдеров, кроме stub (по умолчанию gemini) ---

    @Bean
    @ConditionalOnExpression("'${assessment.ai.active-provider:gemini}' != 'stub'")
    public SpringAiScoringAdapter springAiScoringAdapter(ChatModel chatModel,
                                                         AiProviderService aiProviderService) {
        return new SpringAiScoringAdapter(chatModel, aiProviderService);
    }

    @Bean
    @ConditionalOnExpression("'${assessment.ai.active-provider:gemini}' != 'stub'")
    public SpringAiQuestionGenerationAdapter springAiQuestionGenerationAdapter(
            ChatModel chatModel,
            AiProviderService aiProviderService) {
        return new SpringAiQuestionGenerationAdapter(chatModel, aiProviderService);
    }

    @Bean
    @ConditionalOnExpression("'${assessment.ai.active-provider:gemini}' != 'stub'")
    public SpringAiFollowUpAdapter springAiFollowUpAdapter(ChatModel chatModel,
                                                           AiProviderService aiProviderService) {
        return new SpringAiFollowUpAdapter(chatModel, aiProviderService);
    }
}
