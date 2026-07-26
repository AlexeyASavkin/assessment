package com.assessment.service;

import com.assessment.entity.AiSettings;
import com.assessment.repository.AiSettingsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Сервис управления активным провайдером LLM.
 * Поддерживает переключение между Gemini, GigaChat, OpenRouter и OpenCode.
 * API-ключи хранятся только в переменных окружения (.env).
 */
@Service
public class AiProviderService {

    private final AiSettingsRepository settingsRepository;

    @Value("${assessment.ai.active-provider:gemini}")
    private String defaultProvider;

    /**
     * Конструктор сервиса управления провайдером AI.
     *
     * @param settingsRepository репозиторий настроек AI
     */
    public AiProviderService(AiSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    /**
     * Возвращает текущий активный провайдер LLM.
     * Приоритет отдается значению из базы данных, иначе используется значение по умолчанию.
     *
     * @return название активного провайдера
     */
    public String getActiveProvider() {
        return settingsRepository.findBySettingKey("active_provider")
                .map(AiSettings::getSettingValue)
                .orElse(defaultProvider);
    }

    /**
     * Устанавливает активный провайдер LLM.
     *
     * @param provider название провайдера
     * @throws IllegalArgumentException если указан неизвестный провайдер
     */
    public void setActiveProvider(String provider) {
        if (!provider.equals("gemini") && !provider.equals("gigachat") && !provider.equals("openrouter") && !provider.equals("opencode")) {
            throw new IllegalArgumentException("Неизвестный провайдер: " + provider);
        }
        AiSettings settings = settingsRepository.findBySettingKey("active_provider")
                .orElse(new AiSettings());
        settings.setSettingKey("active_provider");
        settings.setSettingValue(provider);
        settingsRepository.save(settings);
    }

    /**
     * Возвращает API-ключ для указанного провайдера из переменных окружения.
     *
     * @param provider название провайдера (gemini, gigachat, openrouter, opencode)
     * @return API-ключ или пустую строку, если ключ не найден
     */
    public String getApiKey(String provider) {
        String envKey = provider.toUpperCase() + "_API_KEY";
        String fromEnv = System.getenv(envKey);
        return fromEnv != null ? fromEnv : "";
    }
}