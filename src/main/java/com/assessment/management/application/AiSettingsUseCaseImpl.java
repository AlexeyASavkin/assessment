package com.assessment.management.application;

import com.assessment.service.AiProviderService;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Реализация use case управления настройками ИИ.
 *
 * <p>Воспроизводит бизнес-логику {@code AdminController}: все операции чтения
 * и записи активного провайдера и промтов делегируются {@link AiProviderService};
 * список доступных для выбора провайдеров фиксирован
 * (gemini, gigachat, openrouter, opencode, stub).
 */
@Service
public class AiSettingsUseCaseImpl implements AiSettingsUseCase {

    private final AiProviderService aiProviderService;

    public AiSettingsUseCaseImpl(AiProviderService aiProviderService) {
        this.aiProviderService = aiProviderService;
    }

    @Override
    public String getActiveProvider() {
        return aiProviderService.getActiveProvider();
    }

    @Override
    public List<String> getAvailableProviders() {
        return List.of("gemini", "gigachat", "openrouter", "opencode", "stub");
    }

    @Override
    public void setActiveProvider(String provider) {
        aiProviderService.setActiveProvider(provider);
    }

    @Override
    public Map<String, String> getAllPrompts() {
        return aiProviderService.getAllPrompts();
    }

    @Override
    public void setPrompt(String key, String value) {
        aiProviderService.setPrompt(key, value);
    }
}
