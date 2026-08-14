package com.assessment.management.adapter.in;

import com.assessment.dto.ApplicationSummaryDto;
import com.assessment.management.application.ApplicationManagementUseCase;
import com.assessment.management.application.ApplicationSummary;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Тонкий HTTP-адаптер заявок на оценку и админских отчётов (путь {@code /api/admin/applications}).
 * Требует роль ADMIN, делегирует бизнес-логику {@link ApplicationManagementUseCase}.
 */
@RestController
@RequestMapping("/api/admin")
public class ApplicationController {

    private final ApplicationManagementUseCase applicationManagement;

    public ApplicationController(ApplicationManagementUseCase applicationManagement) {
        this.applicationManagement = applicationManagement;
    }

    /**
     * Возвращает список всех заявок на оценку.
     *
     * @return список заявок с HTTP 200
     */
    @GetMapping("/applications")
    public ResponseEntity<List<ApplicationSummaryDto>> listApplications() {
        return ResponseEntity.ok(applicationManagement.listApplications().stream()
                .map(this::toApplicationSummaryDto)
                .collect(Collectors.toList()));
    }

    /**
     * Возвращает развёрнутый отчёт по сессии для админ-панели.
     *
     * @param sessionId идентификатор сессии
     * @return отчёт с HTTP 200 или HTTP 404, если сессия не найдена
     */
    @GetMapping("/applications/{sessionId}/report")
    public ResponseEntity<Map<String, Object>> getApplicationReport(@PathVariable UUID sessionId) {
        return applicationManagement.getAdminReport(sessionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Преобразует сводку заявки из доменной модели в API-DTO.
     *
     * @param s сводка заявки
     * @return DTO заявки
     */
    private ApplicationSummaryDto toApplicationSummaryDto(ApplicationSummary s) {
        return new ApplicationSummaryDto()
                .tokenId(s.tokenId())
                .employeeId(s.employeeId())
                .employeeName(s.employeeName())
                .competencyName(s.competencyName())
                .sessionStatus(s.sessionStatus() != null
                        ? ApplicationSummaryDto.SessionStatusEnum.fromValue(s.sessionStatus()) : null)
                .sessionId(s.sessionId())
                .averageScore(s.averageScore() != null ? s.averageScore().floatValue() : null)
                .passed(s.passed())
                .createdAt(s.createdAt() != null ? s.createdAt().atOffset(ZoneOffset.UTC) : null)
                .completedAt(s.completedAt() != null ? s.completedAt().atOffset(ZoneOffset.UTC) : null);
    }
}