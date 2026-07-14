package com.assessment.service;

import com.assessment.entity.AiSettings;
import com.assessment.repository.AiSettingsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiProviderService {

    private final AiSettingsRepository settingsRepository;

    @Value("${assessment.ai.active-provider:gemini}")
    private String defaultProvider;

    public AiProviderService(AiSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    public String getActiveProvider() {
        return settingsRepository.findBySettingKey("active_provider")
                .map(AiSettings::getSettingValue)
                .orElse(defaultProvider);
    }

    public void setActiveProvider(String provider) {
        if (!provider.equals("gemini") && !provider.equals("gigachat")) {
            throw new IllegalArgumentException("Неизвестный провайдер: " + provider);
        }
        AiSettings settings = settingsRepository.findBySettingKey("active_provider")
                .orElse(new AiSettings());
        settings.setSettingKey("active_provider");
        settings.setSettingValue(provider);
        settingsRepository.save(settings);
    }
}