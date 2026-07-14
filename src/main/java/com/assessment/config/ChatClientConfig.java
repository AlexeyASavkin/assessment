package com.assessment.config;

import com.assessment.service.AiProviderService;
import com.google.genai.Client;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryTemplate;

import chat.giga.springai.GigaChatModel;
import chat.giga.springai.GigaChatOptions;
import chat.giga.springai.api.GigaChatApiProperties;
import chat.giga.springai.api.GigaChatInternalProperties;
import chat.giga.springai.api.auth.GigaChatApiScope;
import chat.giga.springai.api.auth.GigaChatAuthProperties;
import chat.giga.springai.api.chat.GigaChatApi;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatModel routingChatModel(
            AiProviderService aiProviderService,
            ToolCallingManager toolCallingManager,
            RetryTemplate retryTemplate,
            ObservationRegistry observationRegistry) {

        Map<String, ChatModel> delegates = new HashMap<>();

        String geminiKey = aiProviderService.getApiKey("gemini");
        if (geminiKey != null && !geminiKey.isBlank()) {
            Client genAiClient = Client.builder().apiKey(geminiKey).build();
            GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                    .model(GoogleGenAiChatModel.ChatModel.GEMINI_2_0_FLASH)
                    .build();
            GoogleGenAiChatModel geminiModel = GoogleGenAiChatModel.builder()
                    .genAiClient(genAiClient)
                    .options(options)
                    .toolCallingManager(toolCallingManager)
                    .retryTemplate(retryTemplate)
                    .observationRegistry(observationRegistry)
                    .build();
            delegates.put("gemini", geminiModel);
        }

        String gigachatKey = aiProviderService.getApiKey("gigachat");
        if (gigachatKey != null && !gigachatKey.isBlank()) {
            GigaChatAuthProperties auth = GigaChatAuthProperties.builder()
                    .bearer(GigaChatAuthProperties.Bearer.builder()
                            .apiKey(gigachatKey)
                            .build())
                    .scope(GigaChatApiScope.GIGACHAT_API_PERS)
                    .unsafeSsl(true)
                    .build();

            GigaChatInternalProperties internal = new GigaChatInternalProperties();
            internal.setConnectTimeout(Duration.ofSeconds(15));

            GigaChatApiProperties props = GigaChatApiProperties.builder()
                    .baseUrl(GigaChatApi.DEFAULT_BASE_URL)
                    .auth(auth)
                    .internal(internal)
                    .build();

            GigaChatApi api = new GigaChatApi(props);
            GigaChatOptions options = GigaChatOptions.builder()
                    .model(GigaChatApi.ChatModel.GIGA_CHAT_2)
                    .temperature(0.3)
                    .build();
            GigaChatModel gigachatModel = GigaChatModel.builder()
                    .gigaChatApi(api)
                    .internalProperties(internal)
                    .defaultOptions(options)
                    .retryTemplate(retryTemplate)
                    .observationRegistry(observationRegistry)
                    .build();
            delegates.put("gigachat", gigachatModel);
        }

        return new RoutingChatModel(delegates, aiProviderService);
    }

    @Bean
    public ChatClient chatClient(ChatModel routingChatModel, ChatMemory chatMemory) {
        return ChatClient.builder(routingChatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
