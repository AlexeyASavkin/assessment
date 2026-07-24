package com.assessment.service;

import com.assessment.entity.AiSettings;
import com.assessment.repository.AiSettingsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Сервис управления активным провайдером LLM и API-ключами.
 * Поддерживает переключение между Gemini и GigaChat, а также хранение ключей в базе данных.
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
     * @return название активного провайдера (gemini или gigachat)
     */
    public String getActiveProvider() {
        return settingsRepository.findBySettingKey("active_provider")
                .map(AiSettings::getSettingValue)
                .orElse(defaultProvider);
    }

    /**
     * Устанавливает активный провайдер LLM.
     *
     * @param provider название провайдера (gemini или gigachat)
     * @throws IllegalArgumentException если указан неизвестный провайдер
     */
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

    /**
     * Возвращает API-ключ для указанного провайдера.
     * Сначала ищет ключ в базе данных, затем в переменных окружения.
     *
     * @param provider название провайдера (gemini или gigachat)
     * @return API-ключ или пустую строку, если ключ не найден
     */
    public String getApiKey(String provider) {
        String key = provider + "_api_key";
        String fromDb = settingsRepository.findBySettingKey(key)
                .map(AiSettings::getSettingValue)
                .orElse("");
        if (fromDb != null && !fromDb.isBlank()) {
            return fromDb;
        }
        // Fallback to environment variable: GEMINI_API_KEY / GIGACHAT_API_KEY
        String envKey = provider.toUpperCase() + "_API_KEY";
        String fromEnv = System.getenv(envKey);
        return fromEnv != null ? fromEnv : "";
    }

    /**
     * Сохраняет API-ключ для указанного провайдера в базе данных.
     *
     * @param provider название провайдера (gemini или gigachat)
     * @param apiKey   API-ключ для сохранения
     * @throws IllegalArgumentException если указан неизвестный провайдер
     */
    public void setApiKey(String provider, String apiKey) {
        if (!provider.equals("gemini") && !provider.equals("gigachat")) {
            throw new IllegalArgumentException("Неизвестный провайдер: " + provider);
        }
        String key = provider + "_api_key";
        AiSettings settings = settingsRepository.findBySettingKey(key)
                .orElse(new AiSettings());
        settings.setSettingKey(key);
        settings.setSettingValue(apiKey != null ? apiKey : "");
        settingsRepository.save(settings);
    }
}