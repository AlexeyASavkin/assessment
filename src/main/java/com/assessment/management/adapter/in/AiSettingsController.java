package com.assessment.management.adapter.in;

import com.assessment.dto.AiPromptsDto;
import com.assessment.dto.GetAiSettings200ResponseDto;
import com.assessment.dto.UpdateAiSettings200ResponseDto;
import com.assessment.dto.UpdateAiSettingsRequestDto;
import com.assessment.management.application.AiSettingsUseCase;
import com.assessment.service.AiProviderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Тонкий HTTP-адаптер настроек ИИ и редактируемых промтов LLM
 * (пути {@code /api/admin/settings/ai} и {@code /api/admin/settings/ai/prompts}).
 * Требует роль ADMIN, делегирует бизнес-логику {@link AiSettingsUseCase}.
 * Ключи промтов берёт из констант {@link AiProviderService}, чтобы не дублировать строковые литералы.
 */
@RestController
@RequestMapping("/api/admin")
public class AiSettingsController {

    private final AiSettingsUseCase aiSettings;

    public AiSettingsController(AiSettingsUseCase aiSettings) {
        this.aiSettings = aiSettings;
    }

    /**
     * Возвращает текущие настройки провайдера ИИ и список доступных провайдеров.
     *
     * @return карта с полями {@code activeProvider} и {@code availableProviders} с HTTP 200
     */
    @GetMapping("/settings/ai")
    public ResponseEntity<GetAiSettings200ResponseDto> getAiSettings() {
        String activeProvider = aiSettings.getActiveProvider();
        return ResponseEntity.ok(new GetAiSettings200ResponseDto()
                .activeProvider(GetAiSettings200ResponseDto.ActiveProviderEnum.fromValue(activeProvider))
                .availableProviders(aiSettings.getAvailableProviders()));
    }

    /**
     * Обновляет активного провайдера ИИ.
     *
     * @param dto тело запроса с полем {@code activeProvider}
     * @return обновленные настройки с HTTP 200 или HTTP 400 при отсутствии провайдера
     */
    @PutMapping("/settings/ai")
    public ResponseEntity<?> updateAiSettings(@Valid @RequestBody UpdateAiSettingsRequestDto dto) {
        String provider = dto.getActiveProvider().getValue();
        if (provider == null || provider.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        aiSettings.setActiveProvider(provider);
        String activeProvider = aiSettings.getActiveProvider();
        return ResponseEntity.ok(new UpdateAiSettings200ResponseDto()
                .activeProvider(activeProvider)
                .availableProviders(aiSettings.getAvailableProviders()));
    }

    /**
     * Возвращает все промты ИИ (оценки, генерации вопроса, уточняющего вопроса,
     * переоценки и системные промты уточнения/переоценки).
     *
     * @return карта с промтами
     */
    @GetMapping("/settings/ai/prompts")
    public ResponseEntity<AiPromptsDto> getAiPrompts() {
        Map<String, String> prompts = aiSettings.getAllPrompts();
        return ResponseEntity.ok(new AiPromptsDto()
                .promptScoring(prompts.get(AiProviderService.PROMPT_SCORING))
                .promptQuestion(prompts.get(AiProviderService.PROMPT_QUESTION))
                .promptFollowup(prompts.get(AiProviderService.PROMPT_FOLLOWUP))
                .promptRescore(prompts.get(AiProviderService.PROMPT_RESCORE))
                .promptFollowupSystem(prompts.get(AiProviderService.PROMPT_FOLLOWUP_SYSTEM))
                .promptRescoreSystem(prompts.get(AiProviderService.PROMPT_RESCORE_SYSTEM)));
    }

    /**
     * Обновляет промты ИИ. Принимает DTO с полями-промтами.
     * Ключи: {@link AiProviderService#PROMPT_SCORING}, {@link AiProviderService#PROMPT_QUESTION},
     * {@link AiProviderService#PROMPT_FOLLOWUP}, {@link AiProviderService#PROMPT_RESCORE},
     * {@link AiProviderService#PROMPT_FOLLOWUP_SYSTEM}, {@link AiProviderService#PROMPT_RESCORE_SYSTEM}.
     *
     * @param dto карта с промтами
     * @return обновленные промты
     */
    @PutMapping("/settings/ai/prompts")
    public ResponseEntity<AiPromptsDto> updateAiPrompts(@Valid @RequestBody AiPromptsDto dto) {
        if (dto.getPromptScoring() != null) aiSettings.setPrompt(AiProviderService.PROMPT_SCORING, dto.getPromptScoring());
        if (dto.getPromptQuestion() != null) aiSettings.setPrompt(AiProviderService.PROMPT_QUESTION, dto.getPromptQuestion());
        if (dto.getPromptFollowup() != null) aiSettings.setPrompt(AiProviderService.PROMPT_FOLLOWUP, dto.getPromptFollowup());
        if (dto.getPromptRescore() != null) aiSettings.setPrompt(AiProviderService.PROMPT_RESCORE, dto.getPromptRescore());
        if (dto.getPromptFollowupSystem() != null) aiSettings.setPrompt(AiProviderService.PROMPT_FOLLOWUP_SYSTEM, dto.getPromptFollowupSystem());
        if (dto.getPromptRescoreSystem() != null) aiSettings.setPrompt(AiProviderService.PROMPT_RESCORE_SYSTEM, dto.getPromptRescoreSystem());
        return getAiPrompts();
    }
}