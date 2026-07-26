package com.assessment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration;
import chat.giga.springai.autoconfigure.GigaChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration;

/**
 * Главный класс Spring Boot приложения для оценки компетенций сотрудников.
 * <p>
 * Исключает автоконфигурации Google Gemini, GigaChat и OpenAI, чтобы загружать их
 * вручную через {@link com.assessment.config.ChatClientConfig} с учетом
 * выбранного провайдера и настроек rate limiting.
 */
@SpringBootApplication(exclude = {
    GoogleGenAiChatAutoConfiguration.class,
    GigaChatAutoConfiguration.class,
    OpenAiChatAutoConfiguration.class,
    OpenAiAudioSpeechAutoConfiguration.class,
    OpenAiAudioTranscriptionAutoConfiguration.class,
    OpenAiEmbeddingAutoConfiguration.class,
    OpenAiImageAutoConfiguration.class,
    OpenAiModerationAutoConfiguration.class
})
@EnableAsync
public class AssessmentApplication {

    /**
     * Точка входа в приложение. Запускает Spring Boot контекст.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        SpringApplication.run(AssessmentApplication.class, args);
    }
}
