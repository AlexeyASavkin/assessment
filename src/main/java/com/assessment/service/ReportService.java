package com.assessment.service;

import com.assessment.entity.QuestionAttempt;
import com.assessment.entity.Session;
import com.assessment.repository.QuestionAttemptRepository;
import com.assessment.repository.SessionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис формирования итоговых отчетов по сессиям оценки.
 * Агрегирует оценки по компетенциям, определяет уровень сотрудника и формирует рекомендации.
 */
@Service
public class ReportService {

    private final QuestionAttemptRepository questionAttemptRepository;
    private final SessionRepository sessionRepository;

    /**
     * Конструктор сервиса формирования отчетов.
     *
     * @param questionAttemptRepository репозиторий попыток ответов
     * @param sessionRepository         репозиторий сессий оценки
     */
    public ReportService(QuestionAttemptRepository questionAttemptRepository,
                         SessionRepository sessionRepository) {
        this.questionAttemptRepository = questionAttemptRepository;
        this.sessionRepository = sessionRepository;
    }

    /**
     * Формирует полный отчет по сессии оценки.
     * Включает оценки по темам, достигнутые уровни, уточняющие вопросы и общую рекомендацию.
     *
     * @param sessionId идентификатор сессии
     * @return карта с данными отчета
     */
    public Map<String, Object> generateReport(UUID sessionId) {
        Session session = sessionRepository.findById(sessionId).orElseThrow();
        List<QuestionAttempt> attempts = questionAttemptRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);

        List<QuestionAttempt> validAttempts = attempts.stream()
                .filter(a -> Boolean.TRUE.equals(a.getValidJudge()))
                .collect(Collectors.toList());

        Map<String, List<QuestionAttempt>> byTopic = validAttempts.stream()
                .filter(a -> a.getTopic() != null)
                .collect(Collectors.groupingBy(a -> a.getTopic().getId().toString()));

        List<Map<String, Object>> competencyReports = new ArrayList<>();
        BigDecimal totalScore = BigDecimal.ZERO;
        int competencyCount = 0;

        for (Map.Entry<String, List<QuestionAttempt>> entry : byTopic.entrySet()) {
            List<QuestionAttempt> topicAttempts = entry.getValue();

            BigDecimal avgScore = computeTopicAverage(topicAttempts);
            String achievedLevel = determineLevel(avgScore);

            List<String> feedbacks = topicAttempts.stream()
                    .map(QuestionAttempt::getFeedback)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            Map<String, Object> competencyReport = new LinkedHashMap<>();
            competencyReport.put("topicId", entry.getKey());
            competencyReport.put("topicName", topicAttempts.get(0).getTopic().getName());
            competencyReport.put("sectionName", topicAttempts.get(0).getTopic().getSection().getName());
            competencyReport.put("competencyName", topicAttempts.get(0).getTopic().getSection().getCompetency().getName());
            competencyReport.put("averageScore", avgScore);
            competencyReport.put("achievedLevel", achievedLevel);
            competencyReport.put("followUpScores", topicAttempts.stream()
                    .filter(a -> a.getFollowupDepth() > 0)
                    .map(QuestionAttempt::getScore)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
            competencyReport.put("feedbacks", feedbacks);

            competencyReports.add(competencyReport);
            totalScore = totalScore.add(avgScore);
            competencyCount++;
        }

        BigDecimal overallAvg = competencyCount > 0
                ? totalScore.divide(BigDecimal.valueOf(competencyCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        String compositeLevel = determineLevel(overallAvg);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("sessionId", sessionId);
        report.put("employeeName", session.getEmployee().getFullName());
        report.put("competencies", competencyReports);
        report.put("compositeLevel", compositeLevel);
        report.put("overallRecommendation", generateOverallRecommendation(compositeLevel, competencyReports));

        return report;
    }

    /**
     * Расширенный отчёт для администратора: отчёт по темам + все попытки
     * с текстом вопроса, ответом сотрудника, оценкой и feedback.
     *
     * @param sessionId идентификатор сессии
     * @return карта с полным отчётом для админ-панели
     */
    public Map<String, Object> generateAdminReport(UUID sessionId) {
        Session session = sessionRepository.findById(sessionId).orElseThrow();
        Map<String, Object> base = generateReport(sessionId);

        List<QuestionAttempt> attempts = questionAttemptRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        List<Map<String, Object>> attemptDetails = attempts.stream()
                .map(this::toAttemptDetail)
                .collect(Collectors.toList());

        Map<String, Object> report = new LinkedHashMap<>(base);
        report.put("sessionId", sessionId);
        report.put("employeeId", session.getEmployee().getId());
        report.put("employeeName", session.getEmployee().getFullName());
        report.put("competencyName", session.getEmployee().getCompetency() != null
                ? session.getEmployee().getCompetency().getName() : null);
        report.put("sessionStatus", session.getStatus());
        report.put("createdAt", session.getCreatedAt());
        report.put("updatedAt", session.getUpdatedAt());
        report.put("attempts", attemptDetails);
        return report;
    }

    /**
     * Превращает попытку ответа в детальный словарь для отчёта администратора.
     *
     * @param a попытка ответа
     * @return карта с полями попытки
     */
    private Map<String, Object> toAttemptDetail(QuestionAttempt a) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("attemptId", a.getId());
        detail.put("questionText", a.getQuestionText());
        detail.put("rawTranscript", a.getRawTranscript());
        detail.put("finalTranscript", a.getFinalTranscript());
        detail.put("score", a.getScore());
        detail.put("confidence", a.getConfidence());
        detail.put("validJudge", a.getValidJudge());
        detail.put("feedback", a.getFeedback());
        detail.put("followupDepth", a.getFollowupDepth());
        detail.put("followupParentId", a.getFollowupParent() != null ? a.getFollowupParent().getId() : null);
        detail.put("topicId", a.getTopic() != null ? a.getTopic().getId() : null);
        detail.put("topicName", a.getTopic() != null ? a.getTopic().getName() : null);
        detail.put("sectionName", a.getTopic() != null && a.getTopic().getSection() != null
                ? a.getTopic().getSection().getName() : null);
        detail.put("competencyName", a.getTopic() != null
                && a.getTopic().getSection() != null
                && a.getTopic().getSection().getCompetency() != null
                ? a.getTopic().getSection().getCompetency().getName() : null);
        detail.put("createdAt", a.getCreatedAt());
        return detail;
    }

    /**
     * Считает средний балл по основным (не уточняющим) попыткам темы.
     *
     * @param topicAttempts список попыток по теме
     * @return средний балл с округлением до 2 знаков
     */
    private BigDecimal computeTopicAverage(List<QuestionAttempt> topicAttempts) {
        List<QuestionAttempt> mainAttempts = topicAttempts.stream()
                .filter(a -> a.getFollowupDepth() == 0)
                .collect(Collectors.toList());
        return mainAttempts.stream()
                .map(QuestionAttempt::getScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(mainAttempts.size(), 1)), 2, RoundingMode.HALF_UP);
    }

    /**
     * Сводная статистика по сессии для списка заявок:
     * средний балл и итоговый уровень.
     *
     * @param sessionId идентификатор сессии
     * @return запись {@link SessionSummary} (значения null, если оценок нет)
     */
    public SessionSummary computeSummary(UUID sessionId) {
        List<QuestionAttempt> attempts = questionAttemptRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        List<QuestionAttempt> validAttempts = attempts.stream()
                .filter(a -> Boolean.TRUE.equals(a.getValidJudge()))
                .collect(Collectors.toList());

        Map<String, List<QuestionAttempt>> byTopic = validAttempts.stream()
                .filter(a -> a.getTopic() != null)
                .collect(Collectors.groupingBy(a -> a.getTopic().getId().toString()));

        BigDecimal totalScore = BigDecimal.ZERO;
        int topicCount = 0;
        for (List<QuestionAttempt> topicAttempts : byTopic.values()) {
            BigDecimal avg = computeTopicAverage(topicAttempts);
            totalScore = totalScore.add(avg);
            topicCount++;
        }

        if (topicCount == 0) {
            return new SessionSummary(null, null);
        }
        BigDecimal overallAvg = totalScore.divide(BigDecimal.valueOf(topicCount), 2, RoundingMode.HALF_UP);
        return new SessionSummary(overallAvg, determineLevel(overallAvg));
    }

    /**
     * Запись сводной статистики по сессии: средний балл и итоговый уровень.
     */
    public record SessionSummary(BigDecimal averageScore, String compositeLevel) {}

    /**
     * Определяет уровень компетенции на основе среднего балла.
     *
     * @param avgScore средний балл по критерию или компетенции
     * @return уровень: SENIOR, MIDDLE или JUNIOR
     */
    private String determineLevel(BigDecimal avgScore) {
        if (avgScore.compareTo(new BigDecimal("4.3")) >= 0) {
            return "SENIOR";
        } else if (avgScore.compareTo(new BigDecimal("3.5")) >= 0) {
            return "MIDDLE";
        } else {
            return "JUNIOR";
        }
    }

    /**
     * Формирует общую рекомендацию по результатам оценки на основе композитного уровня.
     *
     * @param compositeLevel      итоговый уровень сотрудника
     * @param competencyReports   список отчетов по компетенциям
     * @return текст общей рекомендации
     */
    private String generateOverallRecommendation(String compositeLevel, List<Map<String, Object>> competencyReports) {
        return switch (compositeLevel) {
            case "SENIOR" -> "Сотрудник демонстрирует высокий уровень компетенций. Рекомендуется к повышению.";
            case "MIDDLE" -> "Сотрудник показывает хороший уровень. Рекомендуется развитие в направлении Senior.";
            default -> "Сотруднику рекомендуется дополнительное обучение и развитие базовых компетенций.";
        };
    }
}
