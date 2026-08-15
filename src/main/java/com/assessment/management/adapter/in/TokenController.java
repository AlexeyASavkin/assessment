package com.assessment.management.adapter.in;

import com.assessment.dto.AssessmentInviteTokenDto;
import com.assessment.dto.mapper.InviteTokenMapper;
import com.assessment.management.application.TokenManagementUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Тонкий HTTP-адаптер пригласительных токенов
 * (пути {@code /api/admin/employees/{employeeId}/invite} и {@code /api/admin/tokens}).
 * Требует роль ADMIN, делегирует бизнес-логику {@link TokenManagementUseCase}, маппит DTO через {@link InviteTokenMapper}.
 */
@RestController
@RequestMapping("/api/admin")
public class TokenController {

    private final TokenManagementUseCase tokenManagement;
    private final InviteTokenMapper inviteTokenMapper;

    public TokenController(TokenManagementUseCase tokenManagement, InviteTokenMapper inviteTokenMapper) {
        this.tokenManagement = tokenManagement;
        this.inviteTokenMapper = inviteTokenMapper;
    }

    /**
     * Генерирует одноразовую пригласительную ссылку для сотрудника.
     *
     * @param employeeId идентификатор сотрудника
     * @return пригласительный URL с HTTP 200 или HTTP 404, если сотрудник не найден
     */
    @PostMapping("/employees/{employeeId}/invite")
    public ResponseEntity<String> generateInviteLink(@PathVariable UUID employeeId) {
        return tokenManagement.generateInviteLink(employeeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Возвращает список всех выданных пригласительных токенов.
     *
     * @return список токенов с HTTP 200
     */
    @GetMapping("/tokens")
    public ResponseEntity<List<AssessmentInviteTokenDto>> listTokens() {
        return ResponseEntity.ok(tokenManagement.listTokens().stream()
                .map(inviteTokenMapper::toDto)
                .collect(Collectors.toList()));
    }
}