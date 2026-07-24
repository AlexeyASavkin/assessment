package com.assessment.service;

import com.assessment.entity.QuestionAttempt;
import com.assessment.entity.Session;
import com.assessment.repository.QuestionAttemptRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Сервис оценки ответов сотрудников с помощью LLM.
 * Анализирует ответы на вопросы и выставляет баллы по шкале 0-5 с уровнем уверенности и рекомендацией.
 */
@Service
public class ScoringService {

    private final ChatClient chatClient;
    private final QuestionAttemptRepository questionAttemptRepository;

    /**
     * Конструктор сервиса оценки ответов.
     *
     * @param chatClientProvider           провайдер ChatClient для вызова LLM
     * @param questionAttemptRepository      репозиторий попыток ответов
     */
    public ScoringService(ObjectProvider<ChatClient> chatClientProvider, QuestionAttemptRepository questionAttemptRepository) {
        this.chatClient = chatClientProvider.getIfAvailable();
        this.questionAttemptRepository = questionAttemptRepository;
    }

    /**
     * Оценивает ответ сотрудника с помощью LLM и сохраняет результат.
     *
     * @param session          текущая сессия оценки
     * @param questionText     текст вопроса
     * @param finalTranscript  итоговый транскрипт ответа сотрудника
     * @param criteriaId       идентификатор критерия оценки
     * @param followupDepth    глубина уточняющего вопроса (0 для основного)
     * @param parentAttempt    родительская попытка ответа (для уточняющих вопросов)
     * @return сохраненная попытка ответа с оценкой
     * @throws IllegalStateException если ChatClient не настроен
     */
    public QuestionAttempt scoreAnswer(Session session, String questionText, String finalTranscript,
                                        UUID criteriaId, int followupDepth, QuestionAttempt parentAttempt) {

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
                questionText, finalTranscript);

        if (chatClient == null) {
            throw new IllegalStateException("ChatClient не настроен. Укажите GEMINI_API_KEY для оценки ответов.");
        }
        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        int score = parseScore(response);
        String confidence = parseConfidence(response);
        String feedback = parseFeedback(response);
        boolean validJudge = score != 0;

        QuestionAttempt attempt = QuestionAttempt.builder()
                .session(session)
                .questionText(questionText)
                .finalTranscript(finalTranscript)
                .score(BigDecimal.valueOf(score))
                .confidence(confidence)
                .validJudge(validJudge)
                .feedback(feedback)
                .followupDepth(followupDepth)
                .followupParent(parentAttempt)
                .build();

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
     *
     * @param json JSON-строка
     * @param key  ключ для поиска
     * @return найденное значение или пустую строку, если ключ не найден
     */
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return "";

        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return "";

        int valueStart = json.indexOf("\"", colonIndex + 1);
        if (valueStart == -1) return "";

        int valueEnd = json.indexOf("\"", valueStart + 1);
        if (valueEnd == -1) return "";

        return json.substring(valueStart + 1, valueEnd);
    }
}
