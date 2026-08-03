package com.assessment.ai.adapter;

import com.assessment.ai.domain.ScoreResult;
import com.assessment.ai.port.LlmScoringPort;

import java.util.Locale;

/**
 * Stub-адаптер оценки ответов для тестового режима {@code AI_PROVIDER=stub}.
 *
 * <p>Возвращает оценку по содержимому ответа без вызова внешнего LLM:
 * ответы со словом «слабый» оцениваются в 1 балл (позволяет BDD-сценариям
 * активировать уточняющий вопрос), остальные — в 4 балла. Детерминированные
 * ответы позволяют BDD-тестам проверять HTTP-потоки без реального API-ключа.
 */
public class StubScoringAdapter implements LlmScoringPort {

    @Override
    public ScoreResult score(String questionText, String answerText) {
        if (answerText != null && answerText.toLowerCase(Locale.ROOT).contains("слабый")) {
            return ScoreResult.of(1, "LOW", "Слабый ответ — требуется уточнение.");
        }
        return ScoreResult.of(4, "HIGH", "Хорошее понимание темы.");
    }
}
