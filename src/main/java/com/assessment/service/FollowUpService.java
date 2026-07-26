package com.assessment.service;

import com.assessment.entity.QuestionAttempt;
import com.assessment.repository.QuestionAttemptRepository;
import com.assessment.util.LlmJsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Сервис уточняющих вопросов.
 *
 * <p>После прохождения всех вопросов темы находит основные попытки с оценкой ≤ 2,
 * генерирует уточняющий вопрос через LLM, переоценивает основную попытку с учётом
 * ответа на уточнение.
 *
 * <p>Контракт:
 * <ul>
 *   <li>один уточняющий вопрос на одну основную попытку (depth = 1, без рекурсии);</li>
 *   <li>{@link #generateFollowUpQuestion} возвращает {@code null} при сбое LLM — сессия
 *       продолжается без зависания;</li>
 *   <li>{@link #rescoreMainAttempt} сохраняет {@link QuestionAttempt#getBaseScore()
 *       baseScore} = исходный score (только при первом вызове, idempotent);</li>
 *   <li>{@link #findFollowUpCandidate} отбирает основные попытки с
 *       {@code followupDepth==0 && validJudge==true && score ≤ 2}, без существующего child.</li>
 * </ul>
 */
@Service
public class FollowUpService {

    private static final Logger logger = LoggerFactory.getLogger(FollowUpService.class);

    private final ChatModel chatModel;
    private final AiProviderService aiProviderService;
    private final QuestionAttemptRepository questionAttemptRepository;

    /**
     * Конструктор сервиса уточняющих вопросов.
     *
     * @param chatModelProvider           провайдер ChatModel (lazy)
     * @param aiProviderService           сервис промтов AI
     * @param questionAttemptRepository   репозиторий попыток ответов
     */
    public FollowUpService(
            ObjectProvider<ChatModel> chatModelProvider,
            AiProviderService aiProviderService,
            QuestionAttemptRepository questionAttemptRepository) {
        this.chatModel = chatModelProvider.getIfAvailable();
        this.aiProviderService = aiProviderService;
        this.questionAttemptRepository = questionAttemptRepository;
    }

    /**
     * Генерирует уточняющий вопрос на основе исходного вопроса и слабого ответа сотрудника.
     *
     * @param mainAttempt основная попытка с оценкой ≤ 2
     * @return текст уточняющего вопроса или {@code null} при сбое LLM
     */
    public String generateFollowUpQuestion(QuestionAttempt mainAttempt) {
        if (chatModel == null) {
            logger.warn("ChatModel недоступен — генерация уточняющего вопроса пропущена для attempt {}", mainAttempt.getId());
            return null;
        }
        String prompt = String.format(
                aiProviderService.getPrompt(AiProviderService.PROMPT_FOLLOWUP),
                mainAttempt.getQuestionText(),
                mainAttempt.getFinalTranscript());
        try {
            String text = chatModel.call(new Prompt(new UserMessage(prompt)))
                    .getResult().getOutput().getText();
            return text == null ? null : text.trim();
        } catch (Exception e) {
            logger.error("Ошибка генерации уточняющего вопроса для attempt {}: {}", mainAttempt.getId(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Переоценивает основную попытку с учётом ответа на уточняющий вопрос.
     *
     * <p>Сохраняет {@code baseScore} = исходный {@code score} (только если он ещё не задан —
     * idempotent при гипотетическом повторном вызове). Вызывает LLM с {@link
     * AiProviderService#PROMPT_RESCORE}, парсит JSON того же формата, что и в
     * {@link ScoringService#scoreAnswer}. Обновляет {@code score}, {@code confidence},
     * {@code validJudge}, {@code feedback} и сохраняет основную попытку.
     *
     * <p>При сбое LLM логирует и оставляет оценку без изменений.
     *
     * @param mainAttempt       основная попытка (depth=0)
     * @param followUpAttempt   уточняющая попытка (depth=1)
     */
    public void rescoreMainAttempt(QuestionAttempt mainAttempt, QuestionAttempt followUpAttempt) {
        if (chatModel == null) {
            logger.warn("ChatModel недоступен — переоценка пропущена для attempt {}", mainAttempt.getId());
            return;
        }
        // Сохраняем baseScore только при первом вызове
        if (mainAttempt.getBaseScore() == null && mainAttempt.getScore() != null) {
            mainAttempt.setBaseScore(mainAttempt.getScore());
        }

        String prompt = String.format(
                aiProviderService.getPrompt(AiProviderService.PROMPT_RESCORE),
                mainAttempt.getQuestionText(),
                mainAttempt.getFinalTranscript(),
                followUpAttempt.getQuestionText(),
                followUpAttempt.getFinalTranscript());
        try {
            String response = chatModel.call(new Prompt(new UserMessage(prompt)))
                    .getResult().getOutput().getText();

            int score;
            try {
                score = Integer.parseInt(LlmJsonParser.extractJsonValue(response, "score"));
            } catch (NumberFormatException e) {
                logger.warn("Не удалось распарсить score из ответа LLM у attempt {}: '{}'", mainAttempt.getId(), response);
                return;
            }
            mainAttempt.setScore(BigDecimal.valueOf(score));
            mainAttempt.setConfidence(LlmJsonParser.extractJsonValue(response, "confidence"));
            mainAttempt.setValidJudge(score != 0);
            String feedback = LlmJsonParser.extractJsonValue(response, "feedback");
            if (!feedback.isEmpty()) {
                mainAttempt.setFeedback(feedback);
            }
            questionAttemptRepository.save(mainAttempt);
            logger.info("Переоценка attempt {}: {} -> {}", mainAttempt.getId(), mainAttempt.getBaseScore(), mainAttempt.getScore());
        } catch (Exception e) {
            logger.error("Ошибка переоценки attempt {}: {}", mainAttempt.getId(), e.getMessage(), e);
        }
    }

    /**
     * Находит первую (хронологически) основную попытку в теме, подходящую для уточнения.
     *
     * <p>Критерии:
     * <ul>
     *   <li>{@code sessionId == session.id}, {@code topic.id == topicId};</li>
     *   <li>{@code followupDepth == 0} (это основной, не уточняющий вопрос);</li>
     *   <li>{@code validJudge == true} (оценка валидна);</li>
     *   <li>{@code score != null} (уже оценён);</li>
     *   <li>{@code score ≤ 2} (слабый ответ);</li>
     *   <li>нет существующего child-уточнения (никто ещё не был задан к этой попытке).</li>
     * </ul>
     *
     * @param sessionId идентификатор сессии
     * @param topicId   идентификатор темы
     * @return кандидат или {@code null}
     */
    public QuestionAttempt findFollowUpCandidate(UUID sessionId, UUID topicId) {
        List<QuestionAttempt> attempts = questionAttemptRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);

        // Собираем id родителей, у которых уже есть уточнение
        java.util.Set<UUID> parentsWithChild = new java.util.HashSet<>();
        for (QuestionAttempt a : attempts) {
            if (a.getFollowupParent() != null && a.getFollowupParent().getId() != null) {
                parentsWithChild.add(a.getFollowupParent().getId());
            }
        }

        for (QuestionAttempt a : attempts) {
            if (a.getTopic() == null || !a.getTopic().getId().equals(topicId)) continue;
            if (a.getFollowupDepth() != null && a.getFollowupDepth() > 0) continue;
            if (a.getValidJudge() == null || !a.getValidJudge()) continue;
            if (a.getScore() == null) continue;
            if (a.getScore().compareTo(BigDecimal.valueOf(2)) > 0) continue;
            if (parentsWithChild.contains(a.getId())) continue;
            return a;
        }
        return null;
    }
}