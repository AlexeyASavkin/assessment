package com.assessment.ai.adapter;

import com.assessment.ai.domain.PromptTemplate;
import com.assessment.ai.domain.QuestionResult;
import com.assessment.ai.port.LlmQuestionGenerationPort;
import com.assessment.service.AiProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Адаптер генерации основных вопросов через Spring AI {@link ChatModel}.
 *
 * <p>Реализует выходной порт {@link LlmQuestionGenerationPort}: строит промпт
 * по шаблону {@link AiProviderService#PROMPT_QUESTION} и вызывает LLM.
 * Логика перенесена из {@code QuestionSelector} без изменения семантики
 * (те же сообщения логов и исключений).
 */
public class SpringAiQuestionGenerationAdapter implements LlmQuestionGenerationPort {

    private static final Logger logger = LoggerFactory.getLogger(SpringAiQuestionGenerationAdapter.class);

    private final ChatModel chatModel;
    private final AiProviderService aiProviderService;

    /**
     * Конструктор адаптера генерации вопросов.
     *
     * @param chatModel         модель чата для вызова LLM
     * @param aiProviderService сервис провайдера AI (для чтения промптов из БД)
     */
    public SpringAiQuestionGenerationAdapter(ChatModel chatModel, AiProviderService aiProviderService) {
        this.chatModel = chatModel;
        this.aiProviderService = aiProviderService;
    }

    @Override
    public QuestionResult generateQuestion(String competencyName, String topicName) {
        String prompt = new PromptTemplate(aiProviderService.getPrompt(AiProviderService.PROMPT_QUESTION))
                .format(competencyName, topicName);

        if (chatModel == null) {
            throw new IllegalStateException("ChatModel не настроен. Укажите GEMINI_API_KEY для генерации вопросов.");
        }
        try {
            String text = chatModel.call(new Prompt(new UserMessage(prompt))).getResult().getOutput().getText();
            return QuestionResult.of(text);
        } catch (Exception e) {
            logger.error("Ошибка генерации вопроса через LLM: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка генерации вопроса: " + e.getMessage(), e);
        }
    }
}
