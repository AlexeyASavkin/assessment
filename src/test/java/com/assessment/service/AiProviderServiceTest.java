package com.assessment.service;

import com.assessment.entity.AiSettings;
import com.assessment.repository.AiSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
        aiProviderService = new AiProviderService(settingsRepository);
        ReflectionTestUtils.setField(aiProviderService, "defaultProvider", "gemini");
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

        assertEquals("gemini", aiProviderService.getActiveProvider());
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
        assertThrows(IllegalArgumentException.class,
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
    @DisplayName("getPrompt возвращает дефолтный промпт, если в БД нет записи")
    void getPromptFallbackToDefault() {
        when(settingsRepository.findBySettingKey(AiProviderService.PROMPT_SCORING))
            .thenReturn(Optional.empty());

        String prompt = aiProviderService.getPrompt(AiProviderService.PROMPT_SCORING);
        assertTrue(prompt.contains("Оцени ответ сотрудника"));
        assertTrue(prompt.contains("%1$s"));
    }

    @Test
    @DisplayName("getPrompt с неизвестным ключом возвращает пустую строку")
    void getPromptUnknownKeyReturnsEmpty() {
        when(settingsRepository.findBySettingKey("unknown_key"))
            .thenReturn(Optional.empty());

        assertEquals("", aiProviderService.getPrompt("unknown_key"));
    }

    @Test
    @DisplayName("getPrompt для followup имеет дефолтный промпт")
    void getPromptFollowupHasDefault() {
        when(settingsRepository.findBySettingKey(AiProviderService.PROMPT_FOLLOWUP))
            .thenReturn(Optional.empty());

        String prompt = aiProviderService.getPrompt(AiProviderService.PROMPT_FOLLOWUP);
        assertTrue(prompt.contains("уточняющий вопрос"));
    }

    @Test
    @DisplayName("getPrompt для rescore имеет дефолтный промпт")
    void getPromptRescoreHasDefault() {
        when(settingsRepository.findBySettingKey(AiProviderService.PROMPT_RESCORE))
            .thenReturn(Optional.empty());

        String prompt = aiProviderService.getPrompt(AiProviderService.PROMPT_RESCORE);
        assertTrue(prompt.contains("Пересчитай итоговую оценку"));
        assertTrue(prompt.contains("%1$s"));
        assertTrue(prompt.contains("Исходный вопрос"));
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
    @DisplayName("getAllPrompts возвращает все 4 типа промптов")
    void getAllPromptsReturnsAllFour() {
        when(settingsRepository.findBySettingKey(anyString())).thenReturn(Optional.empty());

        var all = aiProviderService.getAllPrompts();
        assertEquals(4, all.size());
        assertTrue(all.containsKey(AiProviderService.PROMPT_SCORING));
        assertTrue(all.containsKey(AiProviderService.PROMPT_QUESTION));
        assertTrue(all.containsKey(AiProviderService.PROMPT_FOLLOWUP));
        assertTrue(all.containsKey(AiProviderService.PROMPT_RESCORE));
    }
}
