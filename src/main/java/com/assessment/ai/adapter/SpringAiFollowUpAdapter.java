package com.assessment.ai.adapter;

import com.assessment.ai.domain.FollowUpResult;
import com.assessment.ai.domain.PromptTemplate;
import com.assessment.ai.domain.ScoreResult;
import com.assessment.ai.port.LlmFollowUpPort;
import com.assessment.service.AiProviderService;
import com.assessment.util.LlmJsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.Optional;

/**
 * Адаптер уточняющих вопросов через Spring AI {@link ChatModel}.
 *
 * <p>Реализует выходной порт {@link LlmFollowUpPort}: генерация уточняющего
 * вопроса после слабого ответа (оценка ≤ 2) и переоценка основной попытки
 * с учётом ответа на уточнение. Логика перенесена из {@code FollowUpService}
 * без изменения семантики: оба метода возвращают {@link Optional#empty()}
 * при сбое LLM или невозможности распарсить ответ.
 */
public class SpringAiFollowUpAdapter implements LlmFollowUpPort {

    private static final Logger logger = LoggerFactory.getLogger(SpringAiFollowUpAdapter.class);

    private final ChatModel chatModel;
    private final AiProviderService aiProviderService;

    /**
     * Конструктор адаптера уточняющих вопросов.
     *
     * @param chatModel         модель чата для вызова LLM
     * @param aiProviderService сервис провайдера AI (для чтения промптов из БД)
     */
    public SpringAiFollowUpAdapter(ChatModel chatModel, AiProviderService aiProviderService) {
        this.chatModel = chatModel;
        this.aiProviderService = aiProviderService;
    }

    @Override
    public Optional<FollowUpResult> generateFollowUpQuestion(String questionText, String answerText) {
        if (chatModel == null) {
            logger.warn("ChatModel недоступен — генерация уточняющего вопроса пропущена");
            return Optional.empty();
        }
        String prompt = new PromptTemplate(aiProviderService.getPrompt(AiProviderService.PROMPT_FOLLOWUP))
                .format(questionText, answerText);
        try {
            String text = chatModel.call(new Prompt(new UserMessage(prompt)))
                    .getResult().getOutput().getText();
            return text == null ? Optional.empty() : Optional.of(FollowUpResult.of(text.trim()));
        } catch (Exception e) {
            logger.error("Ошибка генерации уточняющего вопроса: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<ScoreResult> rescoreMainAttempt(String questionText, String answerText,
                                                    String followUpQuestionText, String followUpAnswerText) {
        if (chatModel == null) {
            logger.warn("ChatModel недоступен — переоценка пропущена");
            return Optional.empty();
        }
        String prompt = new PromptTemplate(aiProviderService.getPrompt(AiProviderService.PROMPT_RESCORE))
                .format(questionText, answerText, followUpQuestionText, followUpAnswerText);
        try {
            String response = chatModel.call(new Prompt(new UserMessage(prompt)))
                    .getResult().getOutput().getText();

            int score;
            try {
                score = Integer.parseInt(LlmJsonParser.extractJsonValue(response, "score"));
            } catch (NumberFormatException e) {
                logger.warn("Не удалось распарсить score из ответа LLM: '{}'", response);
                return Optional.empty();
            }
            String confidence = LlmJsonParser.extractJsonValue(response, "confidence");
            String feedback = LlmJsonParser.extractJsonValue(response, "feedback");
            return Optional.of(ScoreResult.of(score, confidence, feedback));
        } catch (Exception e) {
            logger.error("Ошибка переоценки: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}
