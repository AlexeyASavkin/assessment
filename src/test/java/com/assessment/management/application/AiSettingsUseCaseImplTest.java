package com.assessment.management.application;

import com.assessment.service.AiProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiSettingsUseCase: управление настройками ИИ")
class AiSettingsUseCaseImplTest {

    @Mock
    private AiProviderService aiProviderService;

    private AiSettingsUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new AiSettingsUseCaseImpl(aiProviderService);
    }

    @Test
    @DisplayName("getAvailableProviders возвращает фиксированный список провайдеров")
    void getAvailableProvidersReturnsFixedList() {
        List<String> providers = useCase.getAvailableProviders();

        assertEquals(List.of("gemini", "gigachat", "openrouter", "opencode"), providers);
        verifyNoInteractions(aiProviderService);
    }
}
