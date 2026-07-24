package com.assessment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration;
import chat.giga.springai.autoconfigure.GigaChatAutoConfiguration;

/**
 * Главный класс Spring Boot приложения для оценки компетенций сотрудников.
 * <p>
 * Исключает автоконфигурации Google Gemini и GigaChat, чтобы загружать их
 * вручную через {@link com.assessment.config.ChatClientConfig} с учетом
 * выбранного провайдера и настроек rate limiting.
 */
@SpringBootApplication(exclude = {
    GoogleGenAiChatAutoConfiguration.class,
    GigaChatAutoConfiguration.class
})
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
