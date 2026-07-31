package com.assessment.ai.adapter;

import com.assessment.ai.domain.FollowUpResult;
import com.assessment.ai.domain.ScoreResult;
import com.assessment.ai.port.LlmFollowUpPort;

import java.util.Optional;

/**
 * Stub-адаптер уточняющих вопросов для тестового режима {@code AI_PROVIDER=stub}.
 *
 * <p>Возвращает фиксированные результаты без вызова внешнего LLM.
 * Значения соответствуют canned-ответам {@code StubChatModel} для сценариев
 * генерации уточняющего вопроса и переоценки основной попытки.
 */
public class StubFollowUpAdapter implements LlmFollowUpPort {

    @Override
    public Optional<FollowUpResult> generateFollowUpQuestion(String questionText, String answerText) {
        return Optional.of(FollowUpResult.of("Что вы знаете о параллельных потоках в Stream API?"));
    }

    @Override
    public Optional<ScoreResult> rescoreMainAttempt(String questionText, String answerText,
                                                    String followUpQuestionText, String followUpAnswerText) {
        return Optional.of(ScoreResult.of(4, "HIGH", "Ответ на уточняющий вопрос раскрыл тему. Оценка повышена."));
    }
}
