package com.assessment.ai.adapter;

import com.assessment.ai.domain.PromptTemplate;
import com.assessment.ai.domain.ScoreResult;
import com.assessment.ai.port.LlmScoringPort;
import com.assessment.service.AiProviderService;
import com.assessment.util.LlmJsonParser;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Адаптер оценки ответов сотрудников через Spring AI {@link ChatModel}.
 *
 * <p>Реализует выходной порт {@link LlmScoringPort}: строит промпт по шаблону
 * {@link AiProviderService#PROMPT_SCORING}, вызывает LLM и парсит JSON-ответ
 * через {@link LlmJsonParser}. Логика перенесена из {@code ScoringService}
 * без изменения семантики (те же fallback-значения и сообщения об ошибках).
 */
public class SpringAiScoringAdapter implements LlmScoringPort {

    private final ChatModel chatModel;
    private final AiProviderService aiProviderService;

    /**
     * Конструктор адаптера оценки ответов.
     *
     * @param chatModel         модель чата для вызова LLM
     * @param aiProviderService сервис провайдера AI (для чтения промптов из БД)
     */
    public SpringAiScoringAdapter(ChatModel chatModel, AiProviderService aiProviderService) {
        this.chatModel = chatModel;
        this.aiProviderService = aiProviderService;
    }

    @Override
    public ScoreResult score(String questionText, String answerText) {
        String prompt = new PromptTemplate(aiProviderService.getPrompt(AiProviderService.PROMPT_SCORING))
                .format(questionText, answerText);

        if (chatModel == null) {
            throw new IllegalStateException("ChatModel не настроен. Укажите GEMINI_API_KEY для оценки ответов.");
        }
        String response = chatModel.call(new Prompt(new UserMessage(prompt))).getResult().getOutput().getText();

        return ScoreResult.of(parseScore(response), parseConfidence(response), parseFeedback(response));
    }

    /**
     * Извлекает оценку из JSON-ответа LLM.
     *
     * @param response JSON-ответ от LLM
     * @return числовая оценка или 0 при ошибке парсинга
     */
    private int parseScore(String response) {
        try {
            String scoreStr = LlmJsonParser.extractJsonValue(response, "score");
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
            return LlmJsonParser.extractJsonValue(response, "confidence");
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
            return LlmJsonParser.extractJsonValue(response, "feedback");
        } catch (Exception e) {
            return "Не удалось сформировать рекомендацию";
        }
    }
}
