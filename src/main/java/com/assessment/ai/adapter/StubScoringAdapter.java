package com.assessment.ai.adapter;

import com.assessment.ai.domain.ScoreResult;
import com.assessment.ai.port.LlmScoringPort;

/**
 * Stub-адаптер оценки ответов для тестового режима {@code AI_PROVIDER=stub}.
 *
 * <p>Возвращает фиксированный результат без вызова внешнего LLM.
 * Детерминированные ответы позволяют BDD-тестам проверять HTTP-потоки
 * без реального API-ключа. Значения соответствуют canned-ответам
 * {@code StubChatModel} для сценария скоринга.
 */
public class StubScoringAdapter implements LlmScoringPort {

    @Override
    public ScoreResult score(String questionText, String answerText) {
        return ScoreResult.of(4, "HIGH", "Хорошее понимание темы.");
    }
}
