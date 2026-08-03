package com.assessment.management.application;

import com.assessment.entity.AssessmentInviteToken;
import com.assessment.entity.Competency;
import com.assessment.entity.Employee;
import com.assessment.entity.Session;
import com.assessment.management.port.out.SessionRepositoryPort;
import com.assessment.management.port.out.TokenRepositoryPort;
import com.assessment.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationManagementUseCase: сводка заявок и админский отчёт")
class ApplicationManagementUseCaseImplTest {

    @Mock
    private TokenRepositoryPort tokenRepositoryPort;

    @Mock
    private SessionRepositoryPort sessionRepositoryPort;

    @Mock
    private ReportService reportService;

    private ApplicationManagementUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ApplicationManagementUseCaseImpl(tokenRepositoryPort, sessionRepositoryPort, reportService);
    }

    @Test
    @DisplayName("listApplications для завершённой сессии вычисляет средний балл и результат")
    void listApplicationsCompletedSession() {
        UUID tokenId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        Instant createdAt = Instant.now().minusSeconds(3600);
        Instant completedAt = Instant.now();
        Competency competency = Competency.builder().name("Java").build();
        Employee employee = Employee.builder().id(employeeId).fullName("Иванов Иван").competency(competency).build();
        Session session = Session.builder().id(sessionId).status("COMPLETED").build();
        session.setUpdatedAt(completedAt);
        AssessmentInviteToken token = AssessmentInviteToken.builder()
                .id(tokenId).employee(employee).session(session).build();
        token.setCreatedAt(createdAt);
        when(tokenRepositoryPort.findAllWithDetails()).thenReturn(List.of(token));
        when(reportService.computeSummary(sessionId))
                .thenReturn(new ReportService.SessionSummary(new BigDecimal("4.25"), true));

        List<ApplicationSummary> result = useCase.listApplications();

        assertEquals(1, result.size());
        ApplicationSummary summary = result.get(0);
        assertEquals(tokenId, summary.tokenId());
        assertEquals(employeeId, summary.employeeId());
        assertEquals("Иванов Иван", summary.employeeName());
        assertEquals("Java", summary.competencyName());
        assertEquals("COMPLETED", summary.sessionStatus());
        assertEquals(sessionId, summary.sessionId());
        assertEquals(new BigDecimal("4.25"), summary.averageScore());
        assertTrue(summary.passed());
        assertEquals(createdAt, summary.createdAt());
        assertEquals(completedAt, summary.completedAt());
        verify(reportService).computeSummary(sessionId);
    }

    @Test
    @DisplayName("listApplications для активной сессии не вычисляет сводку")
    void listApplicationsActiveSession() {
        UUID sessionId = UUID.randomUUID();
        Employee employee = Employee.builder().id(UUID.randomUUID()).fullName("Петров Пётр").build();
        Session session = Session.builder().id(sessionId).status("ACTIVE").build();
        AssessmentInviteToken token = AssessmentInviteToken.builder()
                .id(UUID.randomUUID()).employee(employee).session(session).build();
        when(tokenRepositoryPort.findAllWithDetails()).thenReturn(List.of(token));

        List<ApplicationSummary> result = useCase.listApplications();

        assertEquals(1, result.size());
        ApplicationSummary summary = result.get(0);
        assertEquals("ACTIVE", summary.sessionStatus());
        assertEquals(sessionId, summary.sessionId());
        assertNull(summary.averageScore());
        assertFalse(summary.passed());
        assertNull(summary.completedAt());
        verifyNoInteractions(reportService);
    }

    @Test
    @DisplayName("listApplications для токена без сессии оставляет поля сессии пустыми")
    void listApplicationsWithoutSession() {
        Employee employee = Employee.builder().id(UUID.randomUUID()).fullName("Сидоров Сидор").build();
        AssessmentInviteToken token = AssessmentInviteToken.builder()
                .id(UUID.randomUUID()).employee(employee).build();
        when(tokenRepositoryPort.findAllWithDetails()).thenReturn(List.of(token));

        List<ApplicationSummary> result = useCase.listApplications();

        assertEquals(1, result.size());
        ApplicationSummary summary = result.get(0);
        assertNull(summary.sessionId());
        assertNull(summary.sessionStatus());
        assertNull(summary.averageScore());
        assertFalse(summary.passed());
        assertNull(summary.completedAt());
        assertNull(summary.competencyName());
        verifyNoInteractions(reportService);
    }

    @Test
    @DisplayName("listApplications для токена без сотрудника оставляет поля сотрудника пустыми")
    void listApplicationsWithoutEmployee() {
        AssessmentInviteToken token = AssessmentInviteToken.builder().id(UUID.randomUUID()).build();
        when(tokenRepositoryPort.findAllWithDetails()).thenReturn(List.of(token));

        List<ApplicationSummary> result = useCase.listApplications();

        assertEquals(1, result.size());
        ApplicationSummary summary = result.get(0);
        assertNull(summary.employeeId());
        assertNull(summary.employeeName());
        assertNull(summary.competencyName());
        verifyNoInteractions(reportService);
    }

    @Test
    @DisplayName("listApplications возвращает пустой список, если токенов нет")
    void listApplicationsEmpty() {
        when(tokenRepositoryPort.findAllWithDetails()).thenReturn(List.of());

        List<ApplicationSummary> result = useCase.listApplications();

        assertTrue(result.isEmpty());
        verifyNoInteractions(reportService);
    }

    @Test
    @DisplayName("getAdminReport возвращает отчёт, если сессия существует")
    void getAdminReportSessionExists() {
        UUID sessionId = UUID.randomUUID();
        Map<String, Object> report = Map.of("sessionId", sessionId.toString(), "passed", true);
        when(sessionRepositoryPort.existsById(sessionId)).thenReturn(true);
        when(reportService.generateAdminReport(sessionId)).thenReturn(report);

        Optional<Map<String, Object>> result = useCase.getAdminReport(sessionId);

        assertTrue(result.isPresent());
        assertSame(report, result.get());
        verify(reportService).generateAdminReport(sessionId);
    }

    @Test
    @DisplayName("getAdminReport возвращает пусто, если сессия не найдена")
    void getAdminReportSessionMissing() {
        UUID sessionId = UUID.randomUUID();
        when(sessionRepositoryPort.existsById(sessionId)).thenReturn(false);

        Optional<Map<String, Object>> result = useCase.getAdminReport(sessionId);

        assertTrue(result.isEmpty());
        verifyNoInteractions(reportService);
    }
}
