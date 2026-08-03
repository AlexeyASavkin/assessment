package com.assessment.management.application;

import com.assessment.entity.AssessmentInviteToken;
import com.assessment.entity.Employee;
import com.assessment.entity.Session;
import com.assessment.management.port.out.SessionRepositoryPort;
import com.assessment.management.port.out.TokenRepositoryPort;
import com.assessment.service.ReportService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Реализация use case формирования сводки заявок на оценку и админского отчёта.
 *
 * <p>Воспроизводит бизнес-логику {@code AdminController}: сводка строится по всем
 * выданным пригласительным токенам, для завершённых сессий средний балл и результат
 * прохождения вычисляются через {@link ReportService}. Чтение выполняется в
 * read-only транзакции, т.к. обращается к ленивым ассоциациям сущностей.
 * Зависит от выходных портов {@link TokenRepositoryPort},
 * {@link SessionRepositoryPort} и {@link ReportService}.
 */
@Service
public class ApplicationManagementUseCaseImpl implements ApplicationManagementUseCase {

    private final TokenRepositoryPort tokenRepositoryPort;
    private final SessionRepositoryPort sessionRepositoryPort;
    private final ReportService reportService;

    public ApplicationManagementUseCaseImpl(TokenRepositoryPort tokenRepositoryPort,
                                            SessionRepositoryPort sessionRepositoryPort,
                                            ReportService reportService) {
        this.tokenRepositoryPort = tokenRepositoryPort;
        this.sessionRepositoryPort = sessionRepositoryPort;
        this.reportService = reportService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationSummary> listApplications() {
        List<AssessmentInviteToken> tokens = tokenRepositoryPort.findAllWithDetails();
        List<ApplicationSummary> summaries = new ArrayList<>();
        for (AssessmentInviteToken token : tokens) {
            Employee employee = token.getEmployee();
            Session session = token.getSession();
            UUID sessionId = session != null ? session.getId() : null;
            String sessionStatus = session != null ? session.getStatus() : null;
            Instant completedAt = session != null && "COMPLETED".equals(session.getStatus())
                    ? session.getUpdatedAt() : null;
            String competencyName = employee != null && employee.getCompetency() != null
                    ? employee.getCompetency().getName() : null;
            BigDecimal averageScore = null;
            boolean passed = false;
            if (sessionId != null && "COMPLETED".equals(sessionStatus)) {
                ReportService.SessionSummary summary = reportService.computeSummary(sessionId);
                averageScore = summary.averageScore();
                passed = summary.passed();
            }
            summaries.add(new ApplicationSummary(
                    token.getId(),
                    employee != null ? employee.getId() : null,
                    employee != null ? employee.getFullName() : null,
                    competencyName,
                    sessionStatus,
                    sessionId,
                    averageScore,
                    passed,
                    token.getCreatedAt(),
                    completedAt));
        }
        return summaries;
    }

    @Override
    public Optional<Map<String, Object>> getAdminReport(UUID sessionId) {
        if (!sessionRepositoryPort.existsById(sessionId)) {
            return Optional.empty();
        }
        return Optional.of(reportService.generateAdminReport(sessionId));
    }
}
