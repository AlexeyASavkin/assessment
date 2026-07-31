package com.assessment.service;

import com.assessment.ai.domain.QuestionResult;
import com.assessment.ai.port.LlmQuestionGenerationPort;
import com.assessment.entity.*;
import com.assessment.repository.TopicRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Сервис выбора и генерации вопросов для оценки компетенций сотрудников.
 * Использует LLM для генерации основных и уточняющих вопросов по темам компетенций.
 */
@Service
public class QuestionSelector {

    private final LlmQuestionGenerationPort llmQuestionGenerationPort;
    private final TopicRepository topicRepository;

    /**
     * Конструктор сервиса выбора вопросов.
     *
     * @param llmQuestionGenerationPort    порт генерации вопросов через LLM
     * @param topicRepository              репозиторий тем
     */
    public QuestionSelector(LlmQuestionGenerationPort llmQuestionGenerationPort,
                            TopicRepository topicRepository) {
        this.llmQuestionGenerationPort = llmQuestionGenerationPort;
        this.topicRepository = topicRepository;
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

        QuestionResult result = llmQuestionGenerationPort.generateQuestion(
                topic.getSection().getCompetency().getName(),
                topic.getName());
        return result.getQuestionText();
    }
}
