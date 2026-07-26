package com.assessment.service;

import com.assessment.entity.QuestionAttempt;
import com.assessment.entity.Session;
import com.assessment.repository.QuestionAttemptRepository;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Сервис оценки ответов сотрудников с помощью LLM.
 * Анализирует ответы на вопросы и выставляет баллы по шкале 0-5 с уровнем уверенности и рекомендацией.
 */
@Service
public class ScoringService {

    private final ChatModel chatModel;
    private final QuestionAttemptRepository questionAttemptRepository;

    /**
     * Конструктор сервиса оценки ответов.
     *
     * @param chatModelProvider              провайдер ChatModel для вызова LLM
     * @param questionAttemptRepository      репозиторий попыток ответов
     */
    public ScoringService(ObjectProvider<ChatModel> chatModelProvider,
                          QuestionAttemptRepository questionAttemptRepository) {
        this.chatModel = chatModelProvider.getIfAvailable();
        this.questionAttemptRepository = questionAttemptRepository;
    }

    /**
     * Оценивает ответ сотрудника с помощью LLM и обновляет существующую попытку ответа результатом.
     *
     * @param attempt          существующая попытка ответа (уже сохранена с транскриптом)
     * @return обновленная попытка ответа с оценкой
     * @throws IllegalStateException если ChatModel не настроен
     */
    public QuestionAttempt scoreAnswer(QuestionAttempt attempt) {

        String prompt = String.format("""
                Ты — эксперт по оценке компетенций. Оцени ответ сотрудника.

                Вопрос: %s
                Ответ сотрудника: %s

                Оцени по шкале 0-5:
                - 0 = не удалось оценить (некорректный ответ, не по теме)
                - 1-2 = не соответствует уровню
                - 3 = частично соответствует
                - 4-5 = полностью соответствует

                Дай рекомендацию по развитию.

                Формат ответа JSON:
                {"score": <int>, "confidence": "<HIGH|MEDIUM|LOW>", "feedback": "<recommendation>"}
                """,
                attempt.getQuestionText(), attempt.getFinalTranscript());

        if (chatModel == null) {
            throw new IllegalStateException("ChatModel не настроен. Укажите GEMINI_API_KEY для оценки ответов.");
        }
        String response = chatModel.call(new Prompt(new UserMessage(prompt))).getResult().getOutput().getText();

        int score = parseScore(response);

        attempt.setScore(BigDecimal.valueOf(score));
        attempt.setConfidence(parseConfidence(response));
        attempt.setValidJudge(score != 0);
        attempt.setFeedback(parseFeedback(response));

        return questionAttemptRepository.save(attempt);
    }

    /**
     * Извлекает оценку из JSON-ответа LLM.
     *
     * @param response JSON-ответ от LLM
     * @return числовая оценка или 0 при ошибке парсинга
     */
    private int parseScore(String response) {
        try {
            String scoreStr = extractJsonValue(response, "score");
            return Integer.parseInt(scoreStr);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Извлекает уровень уверенности из JSON-ответа LLM.
     *
     * @param response JSON-ответ от LLM
     * @return уровень уверенности (HIGH, MEDIUM, LOW) или LOW при ошибке
     */
    private String parseConfidence(String response) {
        try {
            return extractJsonValue(response, "confidence");
        } catch (Exception e) {
            return "LOW";
        }
    }

    /**
     * Извлекает рекомендацию из JSON-ответа LLM.
     *
     * @param response JSON-ответ от LLM
     * @return текст рекомендации или сообщение об ошибке при неудаче парсинга
     */
    private String parseFeedback(String response) {
        try {
            return extractJsonValue(response, "feedback");
        } catch (Exception e) {
            return "Не удалось сформировать рекомендацию";
        }
    }

    /**
     * Извлекает строковое значение по ключу из JSON-строки.
     * Поддерживает как строковые значения в кавычках ("key": "value"),
     * так и числовые/булевы без кавычек ("key": 5).
     *
     * @param json JSON-строка
     * @param key  ключ для поиска
     * @return найденное значение или пустая строка, если ключ не найден
     */
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return "";

        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return "";

        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && json.charAt(valueStart) == ' ') {
            valueStart++;
        }
        if (valueStart >= json.length()) return "";

        if (json.charAt(valueStart) == '"') {
            // Строковое значение в кавычках
            valueStart++;
            int valueEnd = json.indexOf('"', valueStart);
            if (valueEnd == -1) return "";
            return json.substring(valueStart, valueEnd);
        } else {
            // Числовое / булево значение без кавычек — до запятой или закрывающей скобки
            int valueEnd = valueStart;
            while (valueEnd < json.length() && json.charAt(valueEnd) != ',' && json.charAt(valueEnd) != '}') {
                valueEnd++;
            }
            return json.substring(valueStart, valueEnd).trim();
        }
    }
}
