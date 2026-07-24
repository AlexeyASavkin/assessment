package com.assessment.service;

import com.assessment.entity.*;
import com.assessment.repository.TopicRepository;
import com.assessment.repository.QuestionAttemptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Сервис выбора и генерации вопросов для оценки компетенций сотрудников.
 * Использует LLM для генерации основных и уточняющих вопросов по темам компетенций.
 */
@Service
public class QuestionSelector {

    private static final Logger logger = LoggerFactory.getLogger(QuestionSelector.class);

    private final ChatModel chatModel;
    private final TopicRepository topicRepository;
    private final QuestionAttemptRepository questionAttemptRepository;
    private final int maxFollowupsPerMain;

    /**
     * Конструктор сервиса выбора вопросов.
     *
     * @param chatModelProvider            провайдер ChatModel для вызова LLM
     * @param topicRepository              репозиторий тем
     * @param questionAttemptRepository    репозиторий попыток ответов
     * @param maxFollowupsPerMain          максимальное количество уточняющих вопросов на один основной
     */
    public QuestionSelector(
            ObjectProvider<ChatModel> chatModelProvider,
            TopicRepository topicRepository,
            QuestionAttemptRepository questionAttemptRepository,
            @Value("${assessment.question.max-followups-per-main}") int maxFollowupsPerMain) {
        this.chatModel = chatModelProvider.getIfAvailable();
        this.topicRepository = topicRepository;
        this.questionAttemptRepository = questionAttemptRepository;
        this.maxFollowupsPerMain = maxFollowupsPerMain;
    }

    /**
     * Генерирует основной вопрос по указанной теме с помощью LLM.
     *
     * @param session  текущая сессия оценки
     * @param topicId  идентификатор темы
     * @return текст сгенерированного вопроса
     */
    public String generateQuestion(Session session, UUID topicId) {
        Topic topic = topicRepository.findById(topicId).orElseThrow();

        String prompt = String.format("""
                Ты — эксперт по оценке компетенций. Сгенерируй вопрос для сотрудника.

                Компетенция: %s
                Тема: %s

                Сгенерируй один вопрос на русском языке для оценки этой темы.
                Вопрос должен быть конкретным и позволять оценить уровень сотрудника.
                Верни ТОЛЬКО текст вопроса без лишних объяснений.
                """,
                topic.getSection().getCompetency().getName(),
                topic.getName());

        if (chatModel == null) {
            throw new IllegalStateException("ChatModel не настроен. Укажите GEMINI_API_KEY для генерации вопросов.");
        }
        try {
            return chatModel.call(new Prompt(new UserMessage(prompt))).getResult().getOutput().getText();
        } catch (Exception e) {
            logger.error("Ошибка генерации вопроса через LLM: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка генерации вопроса: " + e.getMessage(), e);
        }
    }

    /**
     * Генерирует уточняющий вопрос на основе предыдущего ответа сотрудника.
     *
     * @param session          текущая сессия оценки
     * @param previousAttempt  предыдущая попытка ответа
     * @return текст уточняющего вопроса
     */
    public String generateFollowUp(Session session, QuestionAttempt previousAttempt) {
        String prompt = String.format("""
                Сотрудник ответил на вопрос, но ответ требует уточнения.
                Предыдущий вопрос: %s
                Ответ сотрудника: %s

                Задай один уточняющий вопрос на русском языке.
                Верни ТОЛЬКО текст вопроса.
                """,
                previousAttempt.getQuestionText(),
                previousAttempt.getFinalTranscript());

        if (chatModel == null) {
            throw new IllegalStateException("ChatModel не настроен. Укажите GEMINI_API_KEY для генерации вопросов.");
        }
        try {
            return chatModel.call(new Prompt(new UserMessage(prompt))).getResult().getOutput().getText();
        } catch (Exception e) {
            logger.error("Ошибка генерации уточняющего вопроса через LLM: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка генерации уточняющего вопроса: " + e.getMessage(), e);
        }
    }

    /**
     * Определяет, нужно ли задать уточняющий вопрос по указанной теме.
     * Уточняющий вопрос задается только после основного и не более заданного лимита.
     *
     * @param session  текущая сессия оценки
     * @param topicId  идентификатор темы
     * @return true, если уточняющий вопрос нужен
     */
    public boolean shouldAskFollowUp(Session session, UUID topicId) {
        List<QuestionAttempt> attempts = questionAttemptRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());

        long followUpCount = attempts.stream()
                .filter(a -> a.getFollowupDepth() > 0)
                .filter(a -> a.getTopic() != null && a.getTopic().getId().equals(topicId))
                .count();

        QuestionAttempt lastAttempt = attempts.isEmpty() ? null : attempts.get(attempts.size() - 1);
        if (lastAttempt != null && lastAttempt.getFollowupDepth() == 0) {
            return followUpCount < maxFollowupsPerMain;
        }

        return false;
    }
}
