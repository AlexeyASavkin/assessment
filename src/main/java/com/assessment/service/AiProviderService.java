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

    /**
     * Промт генерации уточняющего вопроса (placeholders: %1$s = исходный вопрос, %2$s = ответ сотрудника).
     * Используется когда исходный ответ оценён ≤ 2: LLM анализирует слабый ответ и формулирует уточнение.
     */
    private static final String DEFAULT_PROMPT_FOLLOWUP = """
            Сотрудник дал слабый ответ на вопрос ассессмента. Сформулируй один уточняющий вопрос,
            который позволит сотруднику раскрыть тему глубже и пересдать ответ.

            Исходный вопрос: %1$s
            Ответ сотрудника: %2$s

            Требования к уточняющему вопросу:
            - На русском языке, конкретный и профессиональный.
            - Бьёт в слабое место исходного ответа (то, чего не хватило).
            - Не повторяет исходный вопрос, а развивает его.
            - Позволяет по ответу понять, действительно ли сотрудник владеет темой.

            Верни ТОЛЬКО текст уточняющего вопроса. Без префиксов «Вопрос:», без пояснений.
            """;

    /**
     * Промт переоценки исходного ответа с учётом уточняющего (placeholders:
     * %1$s = исходный вопрос, %2$s = исходный ответ, %3$s = уточняющий вопрос, %4$s = ответ на уточнение).
     * Возвращает JSON того же формата, что PROMPT_SCORING.
     */
    private static final String DEFAULT_PROMPT_RESCORE = """
            Ты — эксперт по оценке компетенций. Сотрудник ответил на основной вопрос слабо (оценка ≤ 2)
            и был задан уточняющий вопрос. Пересчитай итоговую оценку основной попытки с учётом обоих ответов.

            Исходный вопрос: %1$s
            Исходный ответ сотрудника: %2$s
            Уточняющий вопрос: %3$s
            Ответ на уточняющий вопрос: %4$s

            Правила пересчёта:
            - Если ответ на уточнение раскрывает тему — подними оценку пропорционально глубине.
            - Если ответ на уточнение такой же слабый — оставь оценку близкой к исходной.
            - Шкала 0-5: 0 = не удалось оценить; 1-2 = не соответствует; 3 = частично; 4-5 = полностью.
            - Учитывай КАК исходный, ТАК И уточняющий ответ (не заменяй один другим).

            Формат ответа JSON:
            {"score": <int>, "confidence": "<HIGH|MEDIUM|LOW>", "feedback": "<recommendation>"}
            """;

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
     * Возвращает промт по ключу. Если в БД нет сохранённого значения — возвращает дефолт.
     * Значение кэшируется до ближайшего {@link #setPrompt}, чтобы не читать БД на каждый LLM-вызов.
     *
     * @param key ключ промта (PROMPT_SCORING / PROMPT_QUESTION / PROMPT_FOLLOWUP / PROMPT_RESCORE)
     * @return текст промта (с placeholder'ами %1$s, %2$s, ...)
     */
    public String getPrompt(String key) {
        return promptCache.computeIfAbsent(key, this::resolvePrompt);
    }

    private String resolvePrompt(String key) {
        String defaultValue = switch (key) {
            case PROMPT_SCORING -> DEFAULT_PROMPT_SCORING;
            case PROMPT_QUESTION -> DEFAULT_PROMPT_QUESTION;
            case PROMPT_FOLLOWUP -> DEFAULT_PROMPT_FOLLOWUP;
            case PROMPT_RESCORE -> DEFAULT_PROMPT_RESCORE;
            default -> "";
        };
        return settingsRepository.findBySettingKey(key)
                .map(AiSettings::getSettingValue)
                .orElse(defaultValue);
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
     * @return карта со всеми промтами (текущие из БД или дефолтные)
     */
    public java.util.Map<String, String> getAllPrompts() {
        java.util.Map<String, String> all = new java.util.LinkedHashMap<>();
        all.put(PROMPT_SCORING, getPrompt(PROMPT_SCORING));
        all.put(PROMPT_QUESTION, getPrompt(PROMPT_QUESTION));
        all.put(PROMPT_FOLLOWUP, getPrompt(PROMPT_FOLLOWUP));
        all.put(PROMPT_RESCORE, getPrompt(PROMPT_RESCORE));
        return all;
    }
}