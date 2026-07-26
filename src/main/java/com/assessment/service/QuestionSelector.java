package com.assessment.service;

import com.assessment.entity.*;
import com.assessment.repository.TopicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

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
    private final AiProviderService aiProviderService;

    /**
     * Конструктор сервиса выбора вопросов.
     *
     * @param chatModelProvider            провайдер ChatModel для вызова LLM
     * @param topicRepository              репозиторий тем
     * @param aiProviderService            сервис провайдера AI (для чтения промтов из БД)
     */
    public QuestionSelector(
            ObjectProvider<ChatModel> chatModelProvider,
            TopicRepository topicRepository,
            AiProviderService aiProviderService) {
        this.chatModel = chatModelProvider.getIfAvailable();
        this.topicRepository = topicRepository;
        this.aiProviderService = aiProviderService;
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

        String prompt = String.format(aiProviderService.getPrompt(AiProviderService.PROMPT_QUESTION),
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
}
