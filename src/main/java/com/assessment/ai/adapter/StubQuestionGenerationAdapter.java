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

    private int counter = 0;

    @Override
    public synchronized QuestionResult generateQuestion(String competencyName, String topicName) {
        // Детерминированные уникальные вопросы: суффикс с индексом гарантирует, что
        // BDD-сценарий с банком из N вопросов получит N различных текстов и topic
        // не завершится досрочно после первого ответа.
        String question = String.format("Расскажите о вашем опыте работы с %s (вопрос %d).", topicName, ++counter);
        return QuestionResult.of(question);
    }
}
