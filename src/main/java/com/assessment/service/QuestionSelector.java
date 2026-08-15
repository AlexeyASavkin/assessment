package com.assessment.service;

import com.assessment.ai.domain.QuestionResult;
import com.assessment.ai.port.LlmQuestionGenerationPort;
import com.assessment.entity.*;
import com.assessment.repository.TopicRepository;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Сервис выбора и генерации вопросов для оценки компетенций сотрудников.
 * Использует LLM для генерации основных и уточняющих вопросов по темам компетенций.
 * <p>
 * Генерация вопросов (админский сценарий) ограничена общим bucket'ом
 * {@code geminiApi}, чтобы пакетная генерация не превышала лимиты LLM-провайдера.
 */
@Service
public class QuestionSelector {

    private static final Logger logger = LoggerFactory.getLogger(QuestionSelector.class);

    private final LlmQuestionGenerationPort llmQuestionGenerationPort;
    private final TopicRepository topicRepository;
    private final RateLimiterRegistry rateLimiterRegistry;

    /**
     * Конструктор сервиса выбора вопросов.
     *
     * @param llmQuestionGenerationPort    порт генерации вопросов через LLM
     * @param topicRepository              репозиторий тем
     * @param rateLimiterRegistry          реестр rate limiter'ов Resilience4j
     */
    public QuestionSelector(LlmQuestionGenerationPort llmQuestionGenerationPort,
                            TopicRepository topicRepository,
                            RateLimiterRegistry rateLimiterRegistry) {
        this.llmQuestionGenerationPort = llmQuestionGenerationPort;
        this.topicRepository = topicRepository;
        this.rateLimiterRegistry = rateLimiterRegistry;
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
        logger.info("Генерация вопроса: topicId={}, тема='{}'", topicId, topic.getName());

        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("geminiApi");
        try {
            QuestionResult result = RateLimiter.decorateSupplier(rateLimiter, () ->
                    llmQuestionGenerationPort.generateQuestion(
                            topic.getSection().getCompetency().getName(),
                            topic.getName())).get();
            logger.debug("Получен текст вопроса: {}",
                    result.getQuestionText() == null ? "null"
                            : result.getQuestionText().substring(0, Math.min(120, result.getQuestionText().length())));
            return result.getQuestionText();
        } catch (RuntimeException e) {
            logger.error("Ошибка генерации вопроса для темы '{}': {}", topic.getName(), e.getMessage(), e);
            throw e;
        }
    }
}
