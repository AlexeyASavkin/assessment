package com.assessment.service;

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

    /** Промт оценки ответа сотрудника ( placeholders: %1$s = вопрос, %2$s = ответ ). */
    private static final String DEFAULT_PROMPT_SCORING = """
            Ты — эксперт по оценке компетенций. Оцени ответ сотрудника.

            Вопрос: %1$s
            Ответ сотрудника: %2$s

            Оцени по шкале 0-5:
            - 0 = не удалось оценить (некорректный ответ, не по теме)
            - 1-2 = не соответствует уровню
            - 3 = частично соответствует
            - 4-5 = полностью соответствует

            Дай рекомендацию по развитию.

            Формат ответа JSON:
            {"score": <int>, "confidence": "<HIGH|MEDIUM|LOW>", "feedback": "<recommendation>"}
            """;

    /** Промт генерации основного вопроса ( placeholders: %1$s = компетенция, %2$s = тема ). */
    private static final String DEFAULT_PROMPT_QUESTION = """
            Ты — эксперт по оценке компетенций. Сгенерируй вопрос для сотрудника.

            Компетенция: %1$s
            Тема: %2$s

            Сгенерируй один вопрос на русском языке для оценки этой темы.
            Вопрос должен быть конкретным и позволять оценить уровень сотрудника.
            Верни ТОЛЬКО текст вопроса без лишних объяснений.
            """;

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

    // ---- Prompts ----

    /**
     * Возвращает промт по ключу. Если в БД нет сохранённого значения — возвращает дефолт.
     *
     * @param key ключ промта (PROMPT_SCORING / PROMPT_QUESTION)
     * @return текст промта (с placeholder'ами %1$s, %2$s)
     */
    public String getPrompt(String key) {
        String defaultValue = switch (key) {
            case PROMPT_SCORING -> DEFAULT_PROMPT_SCORING;
            case PROMPT_QUESTION -> DEFAULT_PROMPT_QUESTION;
            default -> "";
        };
        return settingsRepository.findBySettingKey(key)
                .map(AiSettings::getSettingValue)
                .orElse(defaultValue);
    }

    /**
     * Сохраняет промт в таблицу ai_settings.
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
    }

    /**
     * Возвращает все промты в виде карты ключ → значение.
     *
     * @return карта со всеми промтами (текущие из БД или дефолтные)
     */
    public java.util.Map<String, String> getAllPrompts() {
        return java.util.Map.of(
                PROMPT_SCORING, getPrompt(PROMPT_SCORING),
                PROMPT_QUESTION, getPrompt(PROMPT_QUESTION)
        );
    }
}