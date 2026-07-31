package com.assessment.assessment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AssessmentSession: неизменяемая доменная модель сессии оценки")
class AssessmentSessionTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID EMPLOYEE_ID = UUID.randomUUID();
    private static final UUID COMPETENCY_ID = UUID.randomUUID();
    private static final UUID QUESTION_ID = UUID.randomUUID();
    private static final Instant CREATED_AT = Instant.now();

    @Test
    @DisplayName("of сохраняет все поля и возвращает их через геттеры")
    void ofStoresAllFields() {
        AssessmentSession session = AssessmentSession.of(ID, EMPLOYEE_ID, "Иванов Иван", COMPETENCY_ID,
                SessionStatus.ACTIVE, QUESTION_ID, CREATED_AT);

        assertEquals(ID, session.getId());
        assertEquals(EMPLOYEE_ID, session.getEmployeeId());
        assertEquals("Иванов Иван", session.getEmployeeName());
        assertEquals(COMPETENCY_ID, session.getCompetencyId());
        assertEquals(SessionStatus.ACTIVE, session.getStatus());
        assertEquals(QUESTION_ID, session.getCurrentQuestionId());
        assertEquals(CREATED_AT, session.getCreatedAt());
    }

    @Test
    @DisplayName("withStatus возвращает новый экземпляр с изменённым статусом, оригинал не меняется")
    void withStatusReturnsNewInstanceAndKeepsOriginal() {
        AssessmentSession original = AssessmentSession.of(ID, EMPLOYEE_ID, "Иванов Иван", COMPETENCY_ID,
                SessionStatus.ACTIVE, QUESTION_ID, CREATED_AT);

        AssessmentSession completed = original.withStatus(SessionStatus.COMPLETED);

        assertNotSame(original, completed);
        assertEquals(SessionStatus.ACTIVE, original.getStatus());
        assertEquals(SessionStatus.COMPLETED, completed.getStatus());
        assertEquals(original.getId(), completed.getId());
        assertEquals(original.getEmployeeId(), completed.getEmployeeId());
        assertEquals(original.getEmployeeName(), completed.getEmployeeName());
        assertEquals(original.getCompetencyId(), completed.getCompetencyId());
        assertEquals(original.getCurrentQuestionId(), completed.getCurrentQuestionId());
        assertEquals(original.getCreatedAt(), completed.getCreatedAt());
    }

    @Test
    @DisplayName("withCurrentQuestionId возвращает новый экземпляр с изменённым вопросом, оригинал не меняется")
    void withCurrentQuestionIdReturnsNewInstanceAndKeepsOriginal() {
        AssessmentSession original = AssessmentSession.of(ID, EMPLOYEE_ID, "Иванов Иван", COMPETENCY_ID,
                SessionStatus.ACTIVE, QUESTION_ID, CREATED_AT);
        UUID newQuestionId = UUID.randomUUID();

        AssessmentSession updated = original.withCurrentQuestionId(newQuestionId);

        assertNotSame(original, updated);
        assertEquals(QUESTION_ID, original.getCurrentQuestionId());
        assertEquals(newQuestionId, updated.getCurrentQuestionId());
        assertEquals(original.getId(), updated.getId());
        assertEquals(original.getStatus(), updated.getStatus());
    }
}
