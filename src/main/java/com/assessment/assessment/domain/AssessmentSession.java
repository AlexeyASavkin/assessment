package com.assessment.assessment.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Доменная модель сессии оценки.
 *
 * <p>Неизменяемая, не содержит JPA/Spring-зависимостей. Агрегирует данные,
 * необходимые use case'ам session flow: идентификатор сотрудника, статус,
 * текущий вопрос.
 */
public final class AssessmentSession {

    private final UUID id;
    private final UUID employeeId;
    private final String employeeName;
    private final UUID competencyId;
    private final SessionStatus status;
    private final UUID currentQuestionId;
    private final Instant createdAt;

    private AssessmentSession(UUID id, UUID employeeId, String employeeName, UUID competencyId,
                              SessionStatus status, UUID currentQuestionId, Instant createdAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.competencyId = competencyId;
        this.status = status;
        this.currentQuestionId = currentQuestionId;
        this.createdAt = createdAt;
    }

    public static AssessmentSession of(UUID id, UUID employeeId, String employeeName, UUID competencyId,
                                       SessionStatus status, UUID currentQuestionId, Instant createdAt) {
        return new AssessmentSession(id, employeeId, employeeName, competencyId, status, currentQuestionId, createdAt);
    }

    public AssessmentSession withStatus(SessionStatus newStatus) {
        return new AssessmentSession(id, employeeId, employeeName, competencyId, newStatus, currentQuestionId, createdAt);
    }

    public AssessmentSession withCurrentQuestionId(UUID newCurrentQuestionId) {
        return new AssessmentSession(id, employeeId, employeeName, competencyId, status, newCurrentQuestionId, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public UUID getCompetencyId() {
        return competencyId;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public UUID getCurrentQuestionId() {
        return currentQuestionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
