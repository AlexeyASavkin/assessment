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

@Service
public class ReportService {

    private final QuestionAttemptRepository questionAttemptRepository;
    private final SessionRepository sessionRepository;

    public ReportService(QuestionAttemptRepository questionAttemptRepository,
                         SessionRepository sessionRepository) {
        this.questionAttemptRepository = questionAttemptRepository;
        this.sessionRepository = sessionRepository;
    }

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

            List<QuestionAttempt> mainAttempts = topicAttempts.stream()
                    .filter(a -> a.getFollowupDepth() == 0)
                    .collect(Collectors.toList());

            List<QuestionAttempt> followUpAttempts = topicAttempts.stream()
                    .filter(a -> a.getFollowupDepth() > 0)
                    .collect(Collectors.toList());

            BigDecimal avgScore = mainAttempts.stream()
                    .map(QuestionAttempt::getScore)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(Math.max(mainAttempts.size(), 1)), 2, RoundingMode.HALF_UP);

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
            competencyReport.put("followUpScores", followUpAttempts.stream()
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

    private String determineLevel(BigDecimal avgScore) {
        if (avgScore.compareTo(new BigDecimal("4.3")) >= 0) {
            return "SENIOR";
        } else if (avgScore.compareTo(new BigDecimal("3.5")) >= 0) {
            return "MIDDLE";
        } else {
            return "JUNIOR";
        }
    }

    private String generateOverallRecommendation(String compositeLevel, List<Map<String, Object>> competencyReports) {
        return switch (compositeLevel) {
            case "SENIOR" -> "Сотрудник демонстрирует высокий уровень компетенций. Рекомендуется к повышению.";
            case "MIDDLE" -> "Сотрудник показывает хороший уровень. Рекомендуется развитие в направлении Senior.";
            default -> "Сотруднику рекомендуется дополнительное обучение и развитие базовых компетенций.";
        };
    }
}
