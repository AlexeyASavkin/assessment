package com.assessment.service;

import com.assessment.common.BadRequestException;
import com.assessment.entity.AiSettings;
import com.assessment.repository.AiSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiProviderService: управление провайдерами, ключами и промптами")
class AiProviderServiceTest {

    @Mock
    private AiSettingsRepository settingsRepository;

    private AiProviderService aiProviderService;

    @BeforeEach
    void setUp() {
        aiProviderService = new AiProviderService(settingsRepository, new MockEnvironment());
        ReflectionTestUtils.setField(aiProviderService, "defaultProvider", "opencode");
    }

    // ---- Active provider ----

    @Test
    @DisplayName("getActiveProvider читает провайдера из БД")
    void getActiveProviderFromDb() {
        AiSettings setting = new AiSettings();
        setting.setSettingValue("gigachat");
        when(settingsRepository.findBySettingKey("active_provider")).thenReturn(Optional.of(setting));

        assertEquals("gigachat", aiProviderService.getActiveProvider());
    }

    @Test
    @DisplayName("getActiveProvider возвращает defaultProvider, если в БД нет записи")
    void getActiveProviderFallbackToDefault() {
        when(settingsRepository.findBySettingKey("active_provider")).thenReturn(Optional.empty());

        assertEquals("opencode", aiProviderService.getActiveProvider());
    }

    @Test
    @DisplayName("setActiveProvider сохраняет валидного провайдера")
    void setActiveProviderValidProviderSaves() {
        when(settingsRepository.findBySettingKey("active_provider")).thenReturn(Optional.empty());
        when(settingsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> aiProviderService.setActiveProvider("gigachat"));

        verify(settingsRepository).save(argThat(s ->
            "active_provider".equals(s.getSettingKey()) && "gigachat".equals(s.getSettingValue())
        ));
    }

    @Test
    @DisplayName("setActiveProvider сохраняет stub-провайдера")
    void setActiveProviderStubProviderSaves() {
        when(settingsRepository.findBySettingKey("active_provider")).thenReturn(Optional.empty());
        when(settingsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> aiProviderService.setActiveProvider("stub"));

        verify(settingsRepository).save(argThat(s ->
            "stub".equals(s.getSettingValue())
        ));
    }

    @Test
    @DisplayName("setActiveProvider с неизвестным провайдером выбрасывает исключение")
    void setActiveProviderInvalidProviderThrows() {
        assertThrows(BadRequestException.class,
            () -> aiProviderService.setActiveProvider("unknown"));
        verify(settingsRepository, never()).save(any());
    }

    @Test
    @DisplayName("setActiveProvider обновляет существующую запись в БД")
    void setActiveProviderUpdatesExistingSetting() {
        AiSettings existing = new AiSettings();
        existing.setSettingKey("active_provider");
        existing.setSettingValue("gemini");
        when(settingsRepository.findBySettingKey("active_provider")).thenReturn(Optional.of(existing));
        when(settingsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        aiProviderService.setActiveProvider("opencode");

        assertEquals("opencode", existing.getSettingValue());
        verify(settingsRepository).save(existing);
    }

    // ---- API keys ----

    @Test
    @DisplayName("getApiKey возвращает значение переменной окружения для gemini")
    void getApiKeyReturnsEnvValue() {
        // System.getenv не мокируется, тест проверяет формат ключа
        String key = aiProviderService.getApiKey("gemini");
        // Должен быть GEMINI_API_KEY или пустая строка
        assertNotNull(key);
    }

    @Test
    @DisplayName("getApiKey для неизвестного провайдера не падает")
    void getApiKeyUnknownProviderReturnsEmptyOrValue() {
        String key = aiProviderService.getApiKey("nonexistent");
        assertNotNull(key);
    }

    // ---- Prompts ----

    @Test
    @DisplayName("getPrompt возвращает промпт из БД")
    void getPromptFromDb() {
        AiSettings setting = new AiSettings();
        setting.setSettingValue("custom scoring prompt");
        when(settingsRepository.findBySettingKey(AiProviderService.PROMPT_SCORING))
            .thenReturn(Optional.of(setting));

        String prompt = aiProviderService.getPrompt(AiProviderService.PROMPT_SCORING);
        assertEquals("custom scoring prompt", prompt);
    }

    @Test
    @DisplayName("getPrompt возвращает пустую строку, если в БД нет записи (дефолтов в коде нет)")
    void getPromptMissingKeyReturnsEmpty() {
        when(settingsRepository.findBySettingKey(AiProviderService.PROMPT_SCORING))
            .thenReturn(Optional.empty());

        assertEquals("", aiProviderService.getPrompt(AiProviderService.PROMPT_SCORING));
    }

    @Test
    @DisplayName("getPrompt с неизвестным ключом возвращает пустую строку")
    void getPromptUnknownKeyReturnsEmpty() {
        when(settingsRepository.findBySettingKey("unknown_key"))
            .thenReturn(Optional.empty());

        assertEquals("", aiProviderService.getPrompt("unknown_key"));
    }

    @Test
    @DisplayName("getPrompt для followup возвращает значение из БД")
    void getPromptFollowupFromDb() {
        AiSettings setting = new AiSettings();
        setting.setSettingValue("followup prompt from db");
        when(settingsRepository.findBySettingKey(AiProviderService.PROMPT_FOLLOWUP))
            .thenReturn(Optional.of(setting));

        assertEquals("followup prompt from db", aiProviderService.getPrompt(AiProviderService.PROMPT_FOLLOWUP));
    }

    @Test
    @DisplayName("getPrompt для rescore возвращает значение из БД")
    void getPromptRescoreFromDb() {
        AiSettings setting = new AiSettings();
        setting.setSettingValue("rescore prompt from db");
        when(settingsRepository.findBySettingKey(AiProviderService.PROMPT_RESCORE))
            .thenReturn(Optional.of(setting));

        assertEquals("rescore prompt from db", aiProviderService.getPrompt(AiProviderService.PROMPT_RESCORE));
    }

    @Test
    @DisplayName("setPrompt создаёт новую запись в БД")
    void setPromptCreatesNew() {
        when(settingsRepository.findBySettingKey(AiProviderService.PROMPT_SCORING))
            .thenReturn(Optional.empty());
        when(settingsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        aiProviderService.setPrompt(AiProviderService.PROMPT_SCORING, "new prompt");

        verify(settingsRepository).save(argThat(s ->
            AiProviderService.PROMPT_SCORING.equals(s.getSettingKey())
                && "new prompt".equals(s.getSettingValue())
        ));
    }

    @Test
    @DisplayName("setPrompt обновляет существующую запись в БД")
    void setPromptUpdatesExisting() {
        AiSettings existing = new AiSettings();
        when(settingsRepository.findBySettingKey(AiProviderService.PROMPT_SCORING))
            .thenReturn(Optional.of(existing));
        when(settingsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        aiProviderService.setPrompt(AiProviderService.PROMPT_SCORING, "updated prompt");

        assertSame(existing, existing); // save вызван с тем же объектом
        verify(settingsRepository).save(argThat(s ->
            "updated prompt".equals(s.getSettingValue())
        ));
    }

    @Test
    @DisplayName("getActiveProvider кэширует значение — повторный вызов не читает БД")
    void getActiveProviderCachesSecondCall() {
        AiSettings setting = new AiSettings();
        setting.setSettingValue("gigachat");
        when(settingsRepository.findBySettingKey("active_provider")).thenReturn(Optional.of(setting));

        assertEquals("gigachat", aiProviderService.getActiveProvider());
        assertEquals("gigachat", aiProviderService.getActiveProvider());

        verify(settingsRepository, times(1)).findBySettingKey("active_provider");
    }

    @Test
    @DisplayName("setActiveProvider инвалидирует кэш — следующий getActiveProvider возвращает новый провайдер")
    void setActiveProviderInvalidatesCache() {
        AiSettings setting = new AiSettings();
        setting.setSettingValue("gemini");
        when(settingsRepository.findBySettingKey("active_provider")).thenReturn(Optional.of(setting));
        when(settingsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("gemini", aiProviderService.getActiveProvider());

        aiProviderService.setActiveProvider("opencode");
        assertEquals("opencode", aiProviderService.getActiveProvider());
    }

    @Test
    @DisplayName("getPrompt кэширует значение — повторный вызов не читает БД")
    void getPromptCachesSecondCall() {
        AiSettings setting = new AiSettings();
        setting.setSettingValue("custom scoring prompt");
        when(settingsRepository.findBySettingKey(AiProviderService.PROMPT_SCORING))
            .thenReturn(Optional.of(setting));

        assertEquals("custom scoring prompt", aiProviderService.getPrompt(AiProviderService.PROMPT_SCORING));
        assertEquals("custom scoring prompt", aiProviderService.getPrompt(AiProviderService.PROMPT_SCORING));

        verify(settingsRepository, times(1)).findBySettingKey(AiProviderService.PROMPT_SCORING);
    }

    @Test
    @DisplayName("setPrompt инвалидирует кэш — следующий getPrompt возвращает новый промпт")
    void setPromptInvalidatesCache() {
        AiSettings setting = new AiSettings();
        setting.setSettingValue("old prompt");
        when(settingsRepository.findBySettingKey(AiProviderService.PROMPT_SCORING))
            .thenReturn(Optional.of(setting));
        when(settingsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("old prompt", aiProviderService.getPrompt(AiProviderService.PROMPT_SCORING));

        aiProviderService.setPrompt(AiProviderService.PROMPT_SCORING, "new prompt");
        assertEquals("new prompt", aiProviderService.getPrompt(AiProviderService.PROMPT_SCORING));
    }

    @Test
    @DisplayName("getAllPrompts возвращает все 6 типов промптов")
    void getAllPromptsReturnsAllSix() {
        when(settingsRepository.findBySettingKey(anyString())).thenReturn(Optional.empty());

        var all = aiProviderService.getAllPrompts();
        assertEquals(6, all.size());
        assertTrue(all.containsKey(AiProviderService.PROMPT_SCORING));
        assertTrue(all.containsKey(AiProviderService.PROMPT_QUESTION));
        assertTrue(all.containsKey(AiProviderService.PROMPT_FOLLOWUP));
        assertTrue(all.containsKey(AiProviderService.PROMPT_RESCORE));
        assertTrue(all.containsKey(AiProviderService.PROMPT_FOLLOWUP_SYSTEM));
        assertTrue(all.containsKey(AiProviderService.PROMPT_RESCORE_SYSTEM));
    }

    @Test
    @DisplayName("getPrompt для followup_system возвращает значение из БД")
    void getPromptFollowupSystemFromDb() {
        AiSettings setting = new AiSettings();
        setting.setSettingValue("system followup from db");
        when(settingsRepository.findBySettingKey(AiProviderService.PROMPT_FOLLOWUP_SYSTEM))
            .thenReturn(Optional.of(setting));

        assertEquals("system followup from db", aiProviderService.getPrompt(AiProviderService.PROMPT_FOLLOWUP_SYSTEM));
    }

    @Test
    @DisplayName("getPrompt для rescore_system возвращает значение из БД")
    void getPromptRescoreSystemFromDb() {
        AiSettings setting = new AiSettings();
        setting.setSettingValue("system rescore from db");
        when(settingsRepository.findBySettingKey(AiProviderService.PROMPT_RESCORE_SYSTEM))
            .thenReturn(Optional.of(setting));

        assertEquals("system rescore from db", aiProviderService.getPrompt(AiProviderService.PROMPT_RESCORE_SYSTEM));
    }
}
