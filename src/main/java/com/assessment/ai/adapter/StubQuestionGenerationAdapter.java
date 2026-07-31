package com.assessment.ai.adapter;

import com.assessment.ai.domain.QuestionResult;
import com.assessment.ai.port.LlmQuestionGenerationPort;

/**
 * Stub-адаптер генерации вопросов для тестового режима {@code AI_PROVIDER=stub}.
 *
 * <p>Возвращает фиксированный вопрос без вызова внешнего LLM.
 * Значение соответствует canned-ответу {@code StubChatModel} для сценария
 * генерации вопроса.
 */
public class StubQuestionGenerationAdapter implements LlmQuestionGenerationPort {

    @Override
    public QuestionResult generateQuestion(String competencyName, String topicName) {
        return QuestionResult.of("Расскажите о вашем опыте работы с данной технологией.");
    }
}
