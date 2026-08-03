package com.assessment.service;

import com.assessment.common.BadRequestException;
import com.assessment.entity.AiSettings;
import com.assessment.repository.AiSettingsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Сервис управления активным провайдером LLM и промтами.
 * Поддерживает переключение между Gemini, GigaChat, OpenRouter и OpenCode.
 * API-ключи хранятся только в переменных окружения (.env).
 * Промты хранятся в таблице ai_settings.
 */
@Service
public class AiProviderService {

    private final AiSettingsRepository settingsRepository;

    /** Ключи настроек для промтов в таблице ai_settings. */
    public static final String PROMPT_SCORING = "prompt_scoring";
    public static final String PROMPT_QUESTION = "prompt_question";
    public static final String PROMPT_FOLLOWUP = "prompt_followup";
    public static final String PROMPT_RESCORE = "prompt_rescore";
    public static final String PROMPT_FOLLOWUP_SYSTEM = "prompt_followup_system";
    public static final String PROMPT_RESCORE_SYSTEM = "prompt_rescore_system";

    @Value("${assessment.ai.active-provider:gemini}")
    private String defaultProvider;

    /** Кэш активного провайдера: сбрасывается при setActiveProvider, чтобы не читать БД на каждый LLM-вызов. */
    private volatile String cachedActiveProvider;

    /** Кэш промптов: сбрасывается при setPrompt. */
    private final java.util.concurrent.ConcurrentHashMap<String, String> promptCache = new java.util.concurrent.ConcurrentHashMap<>();

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
     * Значение кэшируется до ближайшего {@link #setActiveProvider}.
     *
     * @return название активного провайдера
     */
    public String getActiveProvider() {
        String cached = cachedActiveProvider;
        if (cached != null) {
            return cached;
        }
        String resolved = settingsRepository.findBySettingKey("active_provider")
                .map(AiSettings::getSettingValue)
                .orElse(defaultProvider);
        cachedActiveProvider = resolved;
        return resolved;
    }

    /**
     * Устанавливает активный провайдер LLM.
     *
     * @param provider название провайдера
     * @throws IllegalArgumentException если указан неизвестный провайдер
     */
    public void setActiveProvider(String provider) {
        if (!provider.equals("gemini") && !provider.equals("gigachat") && !provider.equals("openrouter") && !provider.equals("opencode") && !provider.equals("stub")) {
            throw new BadRequestException("Неизвестный провайдер: " + provider);
        }
        AiSettings settings = settingsRepository.findBySettingKey("active_provider")
                .orElse(new AiSettings());
        settings.setSettingKey("active_provider");
        settings.setSettingValue(provider);
        settingsRepository.save(settings);
        cachedActiveProvider = provider;
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

    // ---- Prompts ----

    /**
     * Возвращает промт по ключу из таблицы ai_settings.
     * Значение кэшируется до ближайшего {@link #setPrompt}, чтобы не читать БД на каждый LLM-вызов.
     * Начальные значения промтов загружаются в БД миграцией Liquibase (014-ai-prompts.yml).
     *
     * @param key ключ промта (PROMPT_SCORING / PROMPT_QUESTION / PROMPT_FOLLOWUP / PROMPT_RESCORE
     *            / PROMPT_FOLLOWUP_SYSTEM / PROMPT_RESCORE_SYSTEM)
     * @return текст промта (с placeholder'ами %1$s, %2$s, ...) или пустую строку, если записи нет
     */
    public String getPrompt(String key) {
        return promptCache.computeIfAbsent(key, this::resolvePrompt);
    }

    private String resolvePrompt(String key) {
        return settingsRepository.findBySettingKey(key)
                .map(AiSettings::getSettingValue)
                .orElse("");
    }

    /**
     * Сохраняет промт в таблицу ai_settings и обновляет кэш.
     *
     * @param key   ключ промта
     * @param value текст промта (с placeholder'ами)
     */
    public void setPrompt(String key, String value) {
        AiSettings settings = settingsRepository.findBySettingKey(key)
                .orElse(new AiSettings());
        settings.setSettingKey(key);
        settings.setSettingValue(value);
        settingsRepository.save(settings);
        promptCache.put(key, value);
    }

    /**
     * Возвращает все промты в виде карты ключ → значение.
     *
     * @return карта со всеми промтами (значения из ai_settings)
     */
    public java.util.Map<String, String> getAllPrompts() {
        java.util.Map<String, String> all = new java.util.LinkedHashMap<>();
        all.put(PROMPT_SCORING, getPrompt(PROMPT_SCORING));
        all.put(PROMPT_QUESTION, getPrompt(PROMPT_QUESTION));
        all.put(PROMPT_FOLLOWUP, getPrompt(PROMPT_FOLLOWUP));
        all.put(PROMPT_RESCORE, getPrompt(PROMPT_RESCORE));
        all.put(PROMPT_FOLLOWUP_SYSTEM, getPrompt(PROMPT_FOLLOWUP_SYSTEM));
        all.put(PROMPT_RESCORE_SYSTEM, getPrompt(PROMPT_RESCORE_SYSTEM));
        return all;
    }
}