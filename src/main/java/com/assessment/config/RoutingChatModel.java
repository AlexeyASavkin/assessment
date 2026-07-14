package com.assessment.config;

import com.assessment.service.AiProviderService;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.Map;

public class RoutingChatModel implements ChatModel {

    private final Map<String, ChatModel> delegates;
    private final AiProviderService aiProviderService;

    public RoutingChatModel(Map<String, ChatModel> delegates, AiProviderService aiProviderService) {
        this.delegates = delegates;
        this.aiProviderService = aiProviderService;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        String provider = aiProviderService.getActiveProvider();
        ChatModel delegate = delegates.get(provider);
        if (delegate == null) {
            throw new IllegalStateException("AI провайдер '" + provider + "' не доступен. Доступные: " + delegates.keySet());
        }
        return delegate.call(prompt);
    }

    public String getName() {
        return "RoutingChatModel";
    }
}