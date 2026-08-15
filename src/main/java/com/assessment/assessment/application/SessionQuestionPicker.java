package com.assessment.assessment.application;

import com.assessment.assessment.domain.Attempt;
import com.assessment.assessment.domain.QuestionBankQuestion;
import com.assessment.assessment.domain.TopicInfo;
import com.assessment.assessment.port.out.QuestionBankRepositoryPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Вспомогательный компонент выбора вопроса из банка для сессии оценки.
 *
 * <p>Инкапсулирует логику подбора неиспользованного вопроса по теме и
 * определения следующей непройденной темы. Зависит только от
 * {@link QuestionBankRepositoryPort} и доменных моделей.
 */
@Component
public class SessionQuestionPicker {

    private static final Logger logger = LoggerFactory.getLogger(SessionQuestionPicker.class);

    private final QuestionBankRepositoryPort questionBankRepositoryPort;
    private final int maxQuestionsPerSession;

    public SessionQuestionPicker(QuestionBankRepositoryPort questionBankRepositoryPort,
                                 @Value("${assessment.question.max-questions-per-session:20}") int maxQuestionsPerSession) {
        this.questionBankRepositoryPort = questionBankRepositoryPort;
        this.maxQuestionsPerSession = maxQuestionsPerSession;
    }

    /**
     * Проверяет, достигнут ли лимит вопросов сессии.
     *
     * <p>Лимит {@code assessment.question.max-questions-per-session} ограничивает
     * суммарное количество выданных вопросов (основных и уточняющих), не давая
     * сессии расти бесконечно.
     *
     * @param sessionAttempts список всех попыток сессии
     * @return {@code true}, если количество попыток достигло лимита
     */
    public boolean hasReachedQuestionLimit(List<Attempt> sessionAttempts) {
        return sessionAttempts.size() >= maxQuestionsPerSession;
    }

    /**
     * Выбирает первый неиспользованный вопрос из банка для указанной темы.
     *
     * @param topicId         идентификатор темы
     * @param sessionAttempts список уже заданных вопросов в сессии
     * @return текст выбранного вопроса
     * @throws IllegalStateException если для темы нет вопросов или все уже использованы
     */
    public String pickQuestion(UUID topicId, List<Attempt> sessionAttempts) {
        List<QuestionBankQuestion> bank = questionBankRepositoryPort.findByTopicIdOrderBySortOrderAsc(topicId);

        if (bank.isEmpty()) {
            logger.warn("Банк вопросов для темы пуст: topicId={}", topicId);
            throw new IllegalStateException(
                    "Для темы нет сгенерированных вопросов. Администратор должен сгенерировать вопросы перед началом оценки.");
        }

        Set<String> usedTexts = sessionAttempts.stream()
                .filter(a -> a.getTopicId() != null && a.getTopicId().equals(topicId))
                .map(Attempt::getQuestionText)
                .collect(Collectors.toSet());

        logger.debug("Выбор вопроса: topicId={}, размер банка={}, использовано={}", topicId, bank.size(), usedTexts.size());

        for (QuestionBankQuestion bankQuestion : bank) {
            if (!usedTexts.contains(bankQuestion.getQuestionText())) {
                return bankQuestion.getQuestionText();
            }
        }

        logger.warn("Все вопросы банка для темы использованы: topicId={}, размер банка={}", topicId, bank.size());
        throw new IllegalStateException(
                "Все вопросы из банка для темы уже использованы. Администратор должен сгенерировать дополнительные вопросы.");
    }

    /**
     * Проверяет, остались ли неиспользованные вопросы в банке для указанной темы.
     *
     * @param topicId         идентификатор темы
     * @param sessionAttempts список попыток ответов в сессии
     * @return {@code true}, если есть хотя бы один неиспользованный вопрос
     */
    public boolean hasUnused(UUID topicId, List<Attempt> sessionAttempts) {
        List<QuestionBankQuestion> bank = questionBankRepositoryPort.findByTopicIdOrderBySortOrderAsc(topicId);
        Set<String> usedTexts = sessionAttempts.stream()
                .filter(a -> a.getTopicId() != null && a.getTopicId().equals(topicId))
                .map(Attempt::getQuestionText)
                .collect(Collectors.toSet());
        return bank.stream().anyMatch(bq -> !usedTexts.contains(bq.getQuestionText()));
    }

    /**
     * Определяет следующую непройденную тему для сессии.
     *
     * <p>Тема считается пройденной, если существует попытка с непустым
     * {@code finalTranscript}. Неотвеченные попытки не расходуют тему.
     *
     * @param attempts   список всех попыток в сессии
     * @param allTopics  список всех доступных тем
     * @return идентификатор следующей темы или {@code null}, если все темы пройдены
     */
    public UUID findNextTopicId(List<Attempt> attempts, List<TopicInfo> allTopics) {
        Set<UUID> answeredTopicIds = attempts.stream()
                .filter(a -> a.getTopicId() != null)
                .filter(a -> a.getFinalTranscript() != null && !a.getFinalTranscript().isBlank())
                .map(Attempt::getTopicId)
                .collect(Collectors.toSet());

        UUID nextTopicId = allTopics.stream()
                .map(TopicInfo::getId)
                .filter(id -> !answeredTopicIds.contains(id))
                .findFirst()
                .orElse(null);

        logger.debug("Определена следующая тема: topicId={}", nextTopicId);
        return nextTopicId;
    }
}