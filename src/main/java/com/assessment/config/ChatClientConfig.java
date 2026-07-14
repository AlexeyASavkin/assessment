package com.assessment.config;

import com.assessment.service.AiProviderService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatModel routingChatModel(
            ObjectProvider<ChatModel> chatModelProvider,
            AiProviderService aiProviderService) {

        Map<String, ChatModel> delegates = new HashMap<>();

        chatModelProvider.orderedStream().forEach(model -> {
            String name = model.getClass().getSimpleName().toLowerCase();
            if (name.contains("google") || name.contains("gemini")) {
                delegates.put("gemini", model);
            } else if (name.contains("gigachat")) {
                delegates.put("gigachat", model);
            }
        });

        return new RoutingChatModel(delegates, aiProviderService);
    }

    @Bean
    public ChatClient chatClient(ChatModel routingChatModel, ChatMemory chatMemory) {
        return ChatClient.builder(routingChatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}