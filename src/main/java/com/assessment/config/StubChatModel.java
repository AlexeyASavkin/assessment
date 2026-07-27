package com.assessment.config;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Stub-модель для тестового режима {@code AI_PROVIDER=stub}.
 * <p>
 * Возвращает предопределённые ответы без вызова внешнего LLM.
 * Используется для BDD-тестов «чёрного ящика» — детерминированные ответы
 * позволяют проверять корректность HTTP-потоков без реального API-ключа.
 * <p>
 * Логика выбора ответа: по ключевым словам в промпте определяется тип задачи
 * (scoring, rescore, followup generation, question generation) и возвращается
 * соответствующий canned JSON или текст.
 */
public class StubChatModel implements ChatModel {

    @Override
    public ChatResponse call(Prompt prompt) {
        String text = prompt.getContents().toLowerCase();
        String responseContent;

        if (text.contains("пересчитай") || text.contains("итоговую")) {
            // Переоценка основного ответа с учётом уточняющего
            responseContent = """
                    {"score": 4, "confidence": "HIGH", "feedback": "Ответ на уточняющий вопрос раскрыл тему. Оценка повышена."}""";
        } else if (text.contains("уточняющий") || text.contains("сформулируй")) {
            // Генерация уточняющего вопроса
            responseContent = "Что вы знаете о параллельных потоках в Stream API?";
        } else if (text.contains("оцени") || text.contains("шкале")) {
            // Скоринг ответа — возвращаем высокий балл
            responseContent = """
                    {"score": 4, "confidence": "HIGH", "feedback": "Хорошее понимание темы."}""";
        } else {
            // Генерация вопроса
            responseContent = "Расскажите о вашем опыте работы с данной технологией.";
        }

        Generation generation = new Generation(new AssistantMessage(responseContent));
        return new ChatResponse(List.of(generation));
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return Flux.just(call(prompt));
    }
}
