package com.assessment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration;
import chat.giga.springai.autoconfigure.GigaChatAutoConfiguration;

@SpringBootApplication(exclude = {
    GoogleGenAiChatAutoConfiguration.class,
    GigaChatAutoConfiguration.class
})
public class AssessmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssessmentApplication.class, args);
    }
}
