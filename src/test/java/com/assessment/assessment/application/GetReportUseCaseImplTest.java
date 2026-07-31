package com.assessment.assessment.application;

import com.assessment.assessment.domain.AssessmentResult;
import com.assessment.assessment.domain.AssessmentSession;
import com.assessment.assessment.domain.Attempt;
import com.assessment.assessment.domain.SessionStatus;
import com.assessment.assessment.port.out.AttemptRepositoryPort;
import com.assessment.assessment.port.out.SessionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetReportUseCase: формирование итогового отчёта по сессии")
class GetReportUseCaseImplTest {

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final String EMPLOYEE_NAME = "Иванов Иван";
    private static final String PASSED_RECOMMENDATION =
            "Сотрудник успешно прошёл оценку компетенций. Рекомендуется к зачёту.";
    private static final String FAILED_RECOMMENDATION =
            "Сотруднику рекомендуется дополнительное обучение и развитие компетенций.";

    @Mock
    private SessionRepositoryPort sessionRepositoryPort;

    @Mock
    private AttemptRepositoryPort attemptRepositoryPort;

    private GetReportUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetReportUseCaseImpl(sessionRepositoryPort, attemptRepositoryPort);
    }

    @Test
    @DisplayName("Средний балл темы считается только по основным вопросам, граница 3.0 — пройдено")
    void topicAverageUsesMainAttemptsOnlyWithBoundaryPass() {
        UUID topicId = UUID.randomUUID();
        List<Attempt> attempts = List.of(
                attempt(topicId, 0, "4", true, "Хороший ответ"),
                attempt(topicId, 0, "2", true, null),
                attempt(topicId, 1, "5", true, "После уточнения"));
        stubSessionAndAttempts(attempts);

        AssessmentResult result = useCase.getReport(SESSION_ID);

        assertEquals(1, result.getTopics().size());
        AssessmentResult.TopicReport report = result.getTopics().get(0);
        // (4 + 2) / 2 = 3.00; оценка уточняющего вопроса в среднее не входит
        assertEquals(0, report.getAverageScore().compareTo(new BigDecimal("3.00")));
        assertTrue(report.isPassed());
        assertEquals(List.of(new BigDecimal("5")), report.getFollowUpScores());
        assertEquals(List.of("Хороший ответ", "После уточнения"), report.getFeedbacks());
        assertTrue(result.isPassed());
        assertEquals(PASSED_RECOMMENDATION, result.getOverallRecommendation());
    }

    @Test
    @DisplayName("Тема со средним баллом ниже 3.0 не пройдена, рекомендация — обучение")
    void topicBelowThresholdFailsWithTrainingRecommendation() {
        UUID topicId = UUID.randomUUID();
        stubSessionAndAttempts(List.of(
                attempt(topicId, 0, "2", true, "Слабо"),
                attempt(topicId, 0, "1", true, "Очень слабо")));

        AssessmentResult result = useCase.getReport(SESSION_ID);

        AssessmentResult.TopicReport report = result.getTopics().get(0);
        assertEquals(0, report.getAverageScore().compareTo(new BigDecimal("1.50")));
        assertFalse(report.isPassed());
        assertFalse(result.isPassed());
        assertEquals(FAILED_RECOMMENDATION, result.getOverallRecommendation());
    }

    @Test
    @DisplayName("Невалидные попытки (validJudge false или null) исключаются из отчёта по теме")
    void invalidAttemptsAreExcludedFromTopicReport() {
        UUID topicId = UUID.randomUUID();
        stubSessionAndAttempts(List.of(
                attempt(topicId, 0, "4", true, "Валидный"),
                attempt(topicId, 0, "1", false, "Невалидный"),
                attempt(topicId, 0, "1", null, "Без вердикта")));

        AssessmentResult result = useCase.getReport(SESSION_ID);

        assertEquals(1, result.getTopics().size());
        AssessmentResult.TopicReport report = result.getTopics().get(0);
        assertEquals(0, report.getAverageScore().compareTo(new BigDecimal("4.00")));
        assertEquals(List.of("Валидный"), report.getFeedbacks());
    }

    @Test
    @DisplayName("Общий средний балл считается по средним баллам всех тем")
    void overallAverageIsComputedAcrossTopics() {
        UUID firstTopicId = UUID.randomUUID();
        UUID secondTopicId = UUID.randomUUID();
        stubSessionAndAttempts(List.of(
                attempt(firstTopicId, 0, "4", true, null),
                attempt(secondTopicId, 0, "2", true, null)));

        AssessmentResult result = useCase.getReport(SESSION_ID);

        assertEquals(2, result.getTopics().size());
        AssessmentResult.TopicReport first = topicReportOf(result, firstTopicId);
        AssessmentResult.TopicReport second = topicReportOf(result, secondTopicId);
        assertEquals(0, first.getAverageScore().compareTo(new BigDecimal("4.00")));
        assertTrue(first.isPassed());
        assertEquals(0, second.getAverageScore().compareTo(new BigDecimal("2.00")));
        assertFalse(second.isPassed());
        // (4.00 + 2.00) / 2 = 3.00 — общий результат на границе считается пройденным
        assertTrue(result.isPassed());
        assertEquals(PASSED_RECOMMENDATION, result.getOverallRecommendation());
    }

    @Test
    @DisplayName("Без валидных попыток отчёт пустой и сессия не пройдена")
    void noValidAttemptsLeadsToEmptyTopicsAndFailure() {
        stubSessionAndAttempts(List.of(attempt(UUID.randomUUID(), 0, "1", false, "Невалидный")));

        AssessmentResult result = useCase.getReport(SESSION_ID);

        assertTrue(result.getTopics().isEmpty());
        assertFalse(result.isPassed());
        assertEquals(FAILED_RECOMMENDATION, result.getOverallRecommendation());
    }

    @Test
    @DisplayName("Отчёт содержит все попытки сессии и данные сотрудника")
    void resultContainsAllAttemptsAndEmployeeData() {
        UUID topicId = UUID.randomUUID();
        List<Attempt> attempts = List.of(
                attempt(topicId, 0, "4", true, "Валидный"),
                attempt(topicId, 0, "1", false, "Невалидный"));
        stubSessionAndAttempts(attempts);

        AssessmentResult result = useCase.getReport(SESSION_ID);

        assertEquals(SESSION_ID, result.getSessionId());
        assertEquals(EMPLOYEE_NAME, result.getEmployeeName());
        assertSame(attempts, result.getAttempts());
    }

    private void stubSessionAndAttempts(List<Attempt> attempts) {
        AssessmentSession session = AssessmentSession.of(SESSION_ID, UUID.randomUUID(), EMPLOYEE_NAME, null,
                SessionStatus.COMPLETED, null, Instant.now());
        when(sessionRepositoryPort.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(attemptRepositoryPort.findBySessionIdOrderByCreatedAtAsc(SESSION_ID)).thenReturn(attempts);
    }

    private static AssessmentResult.TopicReport topicReportOf(AssessmentResult result, UUID topicId) {
        return result.getTopics().stream()
                .filter(t -> topicId.equals(t.getTopicId()))
                .findFirst()
                .orElseThrow();
    }

    private static Attempt attempt(UUID topicId, int depth, String score, Boolean validJudge, String feedback) {
        return Attempt.of(UUID.randomUUID(), SESSION_ID, "Вопрос", "Транскрипт",
                new BigDecimal(score), null, "high", validJudge, feedback,
                depth, null, topicId, "Тема", "Раздел", "Компетенция", Instant.now());
    }
}
