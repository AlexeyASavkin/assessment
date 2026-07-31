package com.assessment.assessment.application;

import com.assessment.assessment.domain.AssessmentResult;

import java.util.UUID;

/**
 * Use case формирования итогового отчёта по завершённой сессии оценки.
 */
public interface GetReportUseCase {

    /**
     * Формирует отчёт по сессии: средние баллы по темам, прохождение,
     * оценки уточняющих вопросов, feedback и полный список попыток.
     *
     * @param sessionId идентификатор сессии
     * @return доменная модель отчёта {@link AssessmentResult}
     * @throws java.util.NoSuchElementException если сессия не найдена
     */
    AssessmentResult getReport(UUID sessionId);
}
