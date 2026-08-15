package com.assessment.assessment.application;

import com.assessment.assessment.domain.AssessmentResult;
import com.assessment.assessment.domain.AssessmentSession;
import com.assessment.assessment.domain.Attempt;
import com.assessment.assessment.port.out.AttemptRepositoryPort;
import com.assessment.assessment.port.out.SessionRepositoryPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Реализация use case формирования итогового отчёта по завершённой сессии оценки.
 *
 * <p>Агрегирует средние баллы по темам (только по основным, валидным попыткам),
 * определяет прохождение (порог 3.0), собирает оценки уточняющих вопросов и
 * feedback, формирует общую рекомендацию. Зависит только от выходных портов.
 */
@Service
public class GetReportUseCaseImpl implements GetReportUseCase {

    private static final Logger logger = LoggerFactory.getLogger(GetReportUseCaseImpl.class);

    private static final BigDecimal PASS_THRESHOLD = new BigDecimal("3.0");

    private final SessionRepositoryPort sessionRepositoryPort;
    private final AttemptRepositoryPort attemptRepositoryPort;

    public GetReportUseCaseImpl(SessionRepositoryPort sessionRepositoryPort,
                                AttemptRepositoryPort attemptRepositoryPort) {
        this.sessionRepositoryPort = sessionRepositoryPort;
        this.attemptRepositoryPort = attemptRepositoryPort;
    }

    @Override
    public AssessmentResult getReport(UUID sessionId) {
        AssessmentSession session = sessionRepositoryPort.findById(sessionId).orElseThrow();
        List<Attempt> attempts = attemptRepositoryPort.findBySessionIdOrderByCreatedAtAsc(sessionId);

        List<Attempt> validAttempts = attempts.stream()
                .filter(a -> Boolean.TRUE.equals(a.getValidJudge()))
                .collect(Collectors.toList());

        Map<UUID, List<Attempt>> byTopic = validAttempts.stream()
                .filter(a -> a.getTopicId() != null)
                .collect(Collectors.groupingBy(Attempt::getTopicId));

        List<AssessmentResult.TopicReport> topicReports = new ArrayList<>();
        BigDecimal totalScore = BigDecimal.ZERO;
        int competencyCount = 0;

        for (Map.Entry<UUID, List<Attempt>> entry : byTopic.entrySet()) {
            List<Attempt> topicAttempts = entry.getValue();

            BigDecimal avgScore = computeTopicAverage(topicAttempts);
            boolean topicPassed = avgScore.compareTo(PASS_THRESHOLD) >= 0;

            List<String> feedbacks = topicAttempts.stream()
                    .map(Attempt::getFeedback)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            List<BigDecimal> followUpScores = topicAttempts.stream()
                    .filter(a -> a.getFollowupDepth() != null && a.getFollowupDepth() > 0)
                    .map(Attempt::getScore)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            Attempt first = topicAttempts.get(0);
            logger.debug("Тема отчёта: topicName={}, avgScore={}, passed={}", first.getTopicName(), avgScore, topicPassed);
            topicReports.add(AssessmentResult.TopicReport.of(
                    entry.getKey(),
                    first.getTopicName(),
                    first.getSectionName(),
                    first.getCompetencyName(),
                    avgScore,
                    topicPassed,
                    followUpScores,
                    feedbacks));

            totalScore = totalScore.add(avgScore);
            competencyCount++;
        }

        BigDecimal overallAvg = competencyCount > 0
                ? totalScore.divide(BigDecimal.valueOf(competencyCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        boolean passed = overallAvg.compareTo(PASS_THRESHOLD) >= 0;

        logger.info("Сформирован отчёт: sessionId={}, attempts={}, passed={}, overallAvg={}",
                sessionId, attempts.size(), passed, overallAvg);

        return AssessmentResult.of(sessionId, session.getEmployeeName(), passed,
                generateOverallRecommendation(passed), topicReports, attempts);
    }

    /**
     * Считает средний балл по основным (не уточняющим) попыткам темы.
     *
     * @param topicAttempts список попыток по теме
     * @return средний балл с округлением до 2 знаков
     */
    private BigDecimal computeTopicAverage(List<Attempt> topicAttempts) {
        List<Attempt> mainAttempts = topicAttempts.stream()
                .filter(a -> a.getFollowupDepth() != null && a.getFollowupDepth() == 0)
                .collect(Collectors.toList());
        return mainAttempts.stream()
                .map(Attempt::getScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(mainAttempts.size(), 1)), 2, RoundingMode.HALF_UP);
    }

    /**
     * Формирует общую рекомендацию по результатам оценки.
     *
     * @param passed результат прохождения (средний балл ≥ 3.0)
     * @return текст общей рекомендации
     */
    private String generateOverallRecommendation(boolean passed) {
        return passed
                ? "Сотрудник успешно прошёл оценку компетенций. Рекомендуется к зачёту."
                : "Сотруднику рекомендуется дополнительное обучение и развитие компетенций.";
    }
}