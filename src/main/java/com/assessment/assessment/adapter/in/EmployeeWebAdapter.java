package com.assessment.assessment.adapter.in;

import com.assessment.assessment.application.AnswerOutcome;
import com.assessment.assessment.application.GetQuestionUseCase;
import com.assessment.assessment.application.GetReportUseCase;
import com.assessment.assessment.application.InviteEmployeeUseCase;
import com.assessment.assessment.application.InviteOutcome;
import com.assessment.assessment.application.QuestionOutcome;
import com.assessment.assessment.application.SubmitAnswerUseCase;
import com.assessment.assessment.domain.AssessmentResult;
import com.assessment.assessment.domain.AssessmentSession;
import com.assessment.assessment.domain.Attempt;
import com.assessment.assessment.domain.SessionStatus;
import com.assessment.dto.AnswerResponseDto;
import com.assessment.dto.CompletedResponseDto;
import com.assessment.dto.QuestionResponseDto;
import com.assessment.dto.SessionDto;
import com.assessment.dto.SubmitAnswerRequestDto;
import com.assessment.security.EmployeeTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Тонкий веб-адаптер (hexagonal driving adapter) для операций сотрудника.
 * <p>
 * Обрабатывает все запросы по пути {@code /api/employee} и делегирует бизнес-логику
 * use case'ам через их интерфейсы. Не зависит от репозиториев, сущностей или
 * сервисов оценки. Единственная инфраструктурная зависимость —
 * {@link EmployeeTokenService} для валидации cookie сессии.
 */
@RestController
@RequestMapping("/api/employee")
public class EmployeeWebAdapter {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeWebAdapter.class);

    private final EmployeeTokenService tokenService;
    private final InviteEmployeeUseCase inviteEmployeeUseCase;
    private final GetQuestionUseCase getQuestionUseCase;
    private final SubmitAnswerUseCase submitAnswerUseCase;
    private final GetReportUseCase getReportUseCase;

    /**
     * Конструктор с внедрением use case'ов и сервиса cookie.
     *
     * @param tokenService           сервис валидации cookie сессии сотрудника
     * @param inviteEmployeeUseCase  use case валидации пригласительных токенов
     * @param getQuestionUseCase     use case получения текущего вопроса
     * @param submitAnswerUseCase    use case отправки ответа
     * @param getReportUseCase       use case формирования отчёта
     */
    public EmployeeWebAdapter(EmployeeTokenService tokenService,
                              InviteEmployeeUseCase inviteEmployeeUseCase,
                              GetQuestionUseCase getQuestionUseCase,
                              SubmitAnswerUseCase submitAnswerUseCase,
                              GetReportUseCase getReportUseCase) {
        this.tokenService = tokenService;
        this.inviteEmployeeUseCase = inviteEmployeeUseCase;
        this.getQuestionUseCase = getQuestionUseCase;
        this.submitAnswerUseCase = submitAnswerUseCase;
        this.getReportUseCase = getReportUseCase;
    }

    /**
     * Обрабатывает пригласительную ссылку сотрудника.
     * Проверяет токен через use case, устанавливает cookie и перенаправляет
     * на страницу сессии или отчёта (если сессия уже завершена).
     *
     * @param token    пригласительный токен из пути
     * @param request  HTTP-запрос
     * @param response HTTP-ответ для установки cookie
     * @return HTTP 302 с заголовком {@code Location} или HTTP 403 при невалидном токене
     */
    @GetMapping("/invite/{token}")
    public ResponseEntity<Void> handleInvite(@PathVariable String token,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        logger.debug("Обработка пригласительной ссылки: tokenSuffix={}",
                token.length() > 8 ? token.substring(token.length() - 8) : token);
        Optional<InviteOutcome> resultOpt = inviteEmployeeUseCase.validateInvite(token);
        if (resultOpt.isEmpty()) {
            logger.warn("Невалидный пригласительный токен — доступ отклонён: tokenSuffix={}",
                    token.length() > 8 ? token.substring(token.length() - 8) : token);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        InviteOutcome result = resultOpt.get();
        tokenService.addSessionCookie(request, response, result.session());

        if (result.reused() && result.session().getStatus() == SessionStatus.COMPLETED) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "/session/" + result.session().getId() + "/report")
                    .build();
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/session/" + result.session().getId())
                .build();
    }

    /**
     * Возвращает текущую сессию сотрудника по cookie.
     * Если cookie отсутствует или невалидна, возвращает HTTP 401.
     *
     * @param cookieValue значение cookie SESSION_EMPLOYEE (не используется напрямую)
     * @param request     HTTP-запрос для валидации сессии
     * @return текущая сессия с HTTP 200 или HTTP 401
     */
    @PostMapping("/sessions")
    public ResponseEntity<SessionDto> createSession(@CookieValue(value = "SESSION_EMPLOYEE", required = false) String cookieValue,
                                                     HttpServletRequest request) {
        Optional<AssessmentSession> sessionOpt = tokenService.validateSessionCookie(request);
        if (sessionOpt.isPresent()) {
            AssessmentSession session = sessionOpt.get();
            return ResponseEntity.ok(new SessionDto()
                    .id(session.getId())
                    .status(SessionDto.StatusEnum.fromValue(session.getStatus().value())));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    /**
     * Возвращает текущий вопрос для сессии или признак завершения.
     *
     * @param sessionId   идентификатор сессии
     * @param cookieValue значение cookie SESSION_EMPLOYEE (не используется напрямую)
     * @param request     HTTP-запрос для валидации сессии
     * @return данные вопроса (QuestionResponseDto) или признак завершения (CompletedResponseDto) с HTTP 200,
     *         HTTP 401 при несоответствии сессии
     */
    @GetMapping("/sessions/{sessionId}/questions")
    public ResponseEntity<?> getCurrentQuestion(@PathVariable UUID sessionId,
                                                @CookieValue(value = "SESSION_EMPLOYEE", required = false) String cookieValue,
                                                HttpServletRequest request) {
        logger.debug("Запрос текущего вопроса: sessionId={}", sessionId);
        Optional<AssessmentSession> sessionOpt = tokenService.validateSessionCookie(request);
        if (sessionOpt.isEmpty() || !sessionOpt.get().getId().equals(sessionId)) {
            logger.warn("Несоответствие сессии cookie — доступ отклонён: sessionId={}", sessionId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        QuestionOutcome outcome = getQuestionUseCase.getCurrentQuestion(sessionId);
        if (outcome instanceof QuestionOutcome.Completed) {
            return ResponseEntity.ok(new CompletedResponseDto().completed(true));
        }
        QuestionOutcome.Question question = (QuestionOutcome.Question) outcome;
        return ResponseEntity.ok(questionResponse(question.attempt()));
    }

    /**
     * Принимает ответ сотрудника, делегирует оценку use case'у и возвращает
     * следующий вопрос или признак завершения сессии.
     *
     * @param sessionId   идентификатор сессии
     * @param requestDto  тело запроса с полями {@code questionAttemptId}, {@code finalTranscript}
     * @param cookieValue значение cookie SESSION_EMPLOYEE (не используется напрямую)
     * @param request     HTTP-запрос для валидации сессии
     * @return следующий вопрос (QuestionResponseDto для уточняющего или AnswerResponseDto для основного)
     *         или признак завершения (CompletedResponseDto) с HTTP 200, HTTP 401 при несоответствии сессии
     */
    @PostMapping("/sessions/{sessionId}/answers")
    public ResponseEntity<?> submitAnswer(@PathVariable UUID sessionId,
                                          @Valid @RequestBody SubmitAnswerRequestDto requestDto,
                                          @CookieValue(value = "SESSION_EMPLOYEE", required = false) String cookieValue,
                                          HttpServletRequest request) {
        logger.debug("Принят ответ: sessionId={}", sessionId);
        Optional<AssessmentSession> sessionOpt = tokenService.validateSessionCookie(request);
        if (sessionOpt.isEmpty() || !sessionOpt.get().getId().equals(sessionId)) {
            logger.warn("Несоответствие сессии cookie — доступ отклонён: sessionId={}", sessionId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AnswerOutcome outcome = submitAnswerUseCase.submitAnswer(
                sessionId, requestDto.getQuestionAttemptId(), requestDto.getFinalTranscript());

        if (outcome instanceof AnswerOutcome.Completed) {
            return ResponseEntity.ok(new CompletedResponseDto().completed(true));
        }
        AnswerOutcome.NextQuestion next = (AnswerOutcome.NextQuestion) outcome;
        Attempt attempt = next.attempt();

        if (attempt.getFollowupDepth() != null && attempt.getFollowupDepth() > 0) {
            return ResponseEntity.ok(questionResponse(attempt));
        }

        return ResponseEntity.ok(new AnswerResponseDto()
                .nextQuestionId(attempt.getId())
                .nextQuestionText(attempt.getQuestionText())
                .topicId(next.topicId())
                .completed(false)
                .isFollowUp(false));
    }

    /**
     * Возвращает итоговый отчёт по завершённой сессии оценки.
     *
     * @param sessionId   идентификатор сессии
     * @param cookieValue значение cookie SESSION_EMPLOYEE (не используется напрямую)
     * @param request     HTTP-запрос для валидации сессии
     * @return отчёт с HTTP 200, HTTP 401 при несоответствии сессии,
     *         или HTTP 403 если сессия ещё не завершена
     */
    @GetMapping("/sessions/{sessionId}/report")
    public ResponseEntity<Map<String, Object>> getReport(@PathVariable UUID sessionId,
                                                          @CookieValue(value = "SESSION_EMPLOYEE", required = false) String cookieValue,
                                                          HttpServletRequest request) {
        logger.debug("Запрос отчёта: sessionId={}", sessionId);
        Optional<AssessmentSession> sessionOpt = tokenService.validateSessionCookie(request);
        if (sessionOpt.isEmpty() || !sessionOpt.get().getId().equals(sessionId)) {
            logger.warn("Несоответствие сессии cookie — доступ отклонён: sessionId={}", sessionId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AssessmentSession session = sessionOpt.get();
        if (session.getStatus() != SessionStatus.COMPLETED) {
            logger.warn("Отчёт запрошен для незавершённой сессии: sessionId={}", sessionId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Session not completed"));
        }

        AssessmentResult result = getReportUseCase.getReport(sessionId);
        return ResponseEntity.ok(buildReportMap(result));
    }

    /**
     * Формирует DTO текущего вопроса из доменной попытки.
     *
     * @param attempt попытка ответа с вопросом
     * @return DTO с данными вопроса
     */
    private QuestionResponseDto questionResponse(Attempt attempt) {
        return new QuestionResponseDto()
                .questionId(attempt.getId())
                .questionText(attempt.getQuestionText())
                .topicId(attempt.getTopicId())
                .isFollowUp(attempt.getFollowupDepth() != null && attempt.getFollowupDepth() > 0)
                .followupParentId(attempt.getFollowupParentId());
    }

    /**
     * Строит карту отчёта из доменной модели {@link AssessmentResult}.
     * <p>
     * Порядок ключей и значения совпадают со старым ReportService.generateReport.
     *
     * @param result доменная модель отчёта
     * @return карта с данными отчёта для JSON-сериализации
     */
    private Map<String, Object> buildReportMap(AssessmentResult result) {
        List<Map<String, Object>> competencyReports = new ArrayList<>();
        for (AssessmentResult.TopicReport t : result.getTopics()) {
            LinkedHashMap<String, Object> competencyReport = new LinkedHashMap<>();
            competencyReport.put("topicId", t.getTopicId());
            competencyReport.put("topicName", t.getTopicName());
            competencyReport.put("sectionName", t.getSectionName());
            competencyReport.put("competencyName", t.getCompetencyName());
            competencyReport.put("averageScore", t.getAverageScore());
            competencyReport.put("passed", t.isPassed());
            competencyReport.put("followUpScores", t.getFollowUpScores());
            competencyReport.put("feedbacks", t.getFeedbacks());
            competencyReports.add(competencyReport);
        }

        List<Map<String, Object>> attemptDetails = new ArrayList<>();
        for (Attempt a : result.getAttempts()) {
            LinkedHashMap<String, Object> detail = new LinkedHashMap<>();
            detail.put("attemptId", a.getId());
            detail.put("questionText", a.getQuestionText());
            detail.put("finalTranscript", a.getFinalTranscript());
            detail.put("score", a.getScore());
            detail.put("baseScore", a.getBaseScore());
            detail.put("confidence", a.getConfidence());
            detail.put("validJudge", a.getValidJudge());
            detail.put("feedback", a.getFeedback());
            detail.put("followupDepth", a.getFollowupDepth());
            detail.put("followupParentId", a.getFollowupParentId());
            detail.put("topicId", a.getTopicId());
            detail.put("topicName", a.getTopicName());
            detail.put("sectionName", a.getSectionName());
            detail.put("competencyName", a.getCompetencyName());
            detail.put("createdAt", a.getCreatedAt());
            attemptDetails.add(detail);
        }

        LinkedHashMap<String, Object> report = new LinkedHashMap<>();
        report.put("sessionId", result.getSessionId());
        report.put("employeeName", result.getEmployeeName());
        report.put("competencies", competencyReports);
        report.put("passed", result.isPassed());
        report.put("overallRecommendation", result.getOverallRecommendation());
        report.put("attempts", attemptDetails);
        return report;
    }
}