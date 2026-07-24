package com.assessment.controller;

import com.assessment.entity.*;
import com.assessment.repository.*;
import com.assessment.security.EmployeeTokenService;
import com.assessment.service.ReportService;
import com.assessment.service.ScoringService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/employee")
public class EmployeeController {

    private final EmployeeTokenService tokenService;
    private final SessionRepository sessionRepository;
    private final ScoringService scoringService;
    private final QuestionAttemptRepository questionAttemptRepository;
    private final QuestionBankRepository questionBankRepository;
    private final TopicRepository topicRepository;
    private final ReportService reportService;

    public EmployeeController(EmployeeTokenService tokenService,
                              SessionRepository sessionRepository,
                              ScoringService scoringService,
                              QuestionAttemptRepository questionAttemptRepository,
                              QuestionBankRepository questionBankRepository,
                              TopicRepository topicRepository,
                              ReportService reportService) {
        this.tokenService = tokenService;
        this.sessionRepository = sessionRepository;
        this.scoringService = scoringService;
        this.questionAttemptRepository = questionAttemptRepository;
        this.questionBankRepository = questionBankRepository;
        this.topicRepository = topicRepository;
        this.reportService = reportService;
    }

    @GetMapping("/invite/{token}")
    public ResponseEntity<Void> handleInvite(@PathVariable String token,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        Optional<EmployeeTokenService.InviteResult> resultOpt = tokenService.validateInviteToken(token);
        if (resultOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        EmployeeTokenService.InviteResult result = resultOpt.get();
        tokenService.addSessionCookie(request, response, result.session());

        if (result.reused() && "COMPLETED".equals(result.session().getStatus())) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "/session/" + result.session().getId() + "/report")
                    .build();
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/session/" + result.session().getId())
                .build();
    }

    @PostMapping("/sessions")
    public ResponseEntity<Session> createSession(@CookieValue(value = "SESSION_EMPLOYEE", required = false) String cookieValue,
                                                  HttpServletRequest request) {
        Optional<Session> sessionOpt = tokenService.validateSessionCookie(request);
        if (sessionOpt.isPresent()) {
            return ResponseEntity.ok(sessionOpt.get());
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping("/sessions/{sessionId}/questions")
    public ResponseEntity<Map<String, Object>> getCurrentQuestion(@PathVariable UUID sessionId,
                                                                   @CookieValue(value = "SESSION_EMPLOYEE", required = false) String cookieValue,
                                                                   HttpServletRequest request) {
        Optional<Session> sessionOpt = tokenService.validateSessionCookie(request);
        if (sessionOpt.isEmpty() || !sessionOpt.get().getId().equals(sessionId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Session session = sessionOpt.get();
        if ("COMPLETED".equals(session.getStatus())) {
            return ResponseEntity.ok(Map.of("completed", true));
        }

        // Idempotent: if session already has a current question, return it without creating a new one.
        // This prevents StrictMode double-fired useEffect (and page reloads) from "using up" topics.
        UUID currentQuestionId = session.getCurrentQuestionId();
        if (currentQuestionId != null) {
            QuestionAttempt current = questionAttemptRepository.findById(currentQuestionId).orElse(null);
            if (current != null) {
                return buildQuestionResponse(current);
            }
        }

        List<QuestionAttempt> attempts = questionAttemptRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);

        // Filter topics by employee's selected competency
        Competency employeeCompetency = session.getEmployee().getCompetency();
        List<Topic> topics;
        if (employeeCompetency != null) {
            UUID competencyId = employeeCompetency.getId();
            topics = topicRepository.findAll().stream()
                    .filter(t -> t.getSection().getCompetency().getId().equals(competencyId))
                    .toList();
        } else {
            topics = topicRepository.findAll();
        }

        UUID nextTopicId = findNextTopicId(attempts, topics);
        if (nextTopicId == null) {
            return ResponseEntity.ok(Map.of("completed", true));
        }

        String questionText = pickQuestionFromBank(nextTopicId, attempts);

        QuestionAttempt attempt = QuestionAttempt.builder()
                .session(session)
                .questionText(questionText)
                .topic(topicRepository.findById(nextTopicId).orElse(null))
                .followupDepth(0)
                .build();
        attempt = questionAttemptRepository.save(attempt);

        session.setCurrentQuestionId(attempt.getId());
        sessionRepository.save(session);

        return buildQuestionResponse(attempt);
    }

    private ResponseEntity<Map<String, Object>> buildQuestionResponse(QuestionAttempt attempt) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("questionId", attempt.getId());
        response.put("questionText", attempt.getQuestionText());
        response.put("topicId", attempt.getTopic() != null ? attempt.getTopic().getId() : null);
        response.put("isFollowUp", attempt.getFollowupDepth() > 0);
        if (attempt.getFollowupParent() != null) {
            response.put("followupParentId", attempt.getFollowupParent().getId());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sessions/{sessionId}/answers")
    public ResponseEntity<Map<String, Object>> submitAnswer(@PathVariable UUID sessionId,
                                                             @RequestBody Map<String, String> answer,
                                                             @CookieValue(value = "SESSION_EMPLOYEE", required = false) String cookieValue,
                                                             HttpServletRequest request) {
        Optional<Session> sessionOpt = tokenService.validateSessionCookie(request);
        if (sessionOpt.isEmpty() || !sessionOpt.get().getId().equals(sessionId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Session session = sessionOpt.get();
        UUID questionId = UUID.fromString(answer.get("questionAttemptId"));
        String rawTranscript = answer.get("rawTranscript");
        String finalTranscript = answer.get("finalTranscript");

        QuestionAttempt currentAttempt = questionAttemptRepository.findById(questionId).orElseThrow();

        currentAttempt.setRawTranscript(rawTranscript);
        currentAttempt.setFinalTranscript(finalTranscript);
        questionAttemptRepository.save(currentAttempt);

        scoringService.scoreAnswer(
                session,
                currentAttempt.getQuestionText(),
                finalTranscript,
                currentAttempt.getTopic() != null ? currentAttempt.getTopic().getId() : null,
                currentAttempt.getFollowupDepth(),
                currentAttempt.getFollowupParent()
        );

        List<QuestionAttempt> allAttempts = questionAttemptRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);

        // Filter topics by employee's selected competency
        Competency employeeCompetency = session.getEmployee().getCompetency();
        List<Topic> topics;
        if (employeeCompetency != null) {
            UUID competencyId = employeeCompetency.getId();
            topics = topicRepository.findAll().stream()
                    .filter(t -> t.getSection().getCompetency().getId().equals(competencyId))
                    .toList();
        } else {
            topics = topicRepository.findAll();
        }

        UUID nextTopicId = findNextTopicId(allAttempts, topics);

        if (nextTopicId == null) {
            session.setStatus("COMPLETED");
            sessionRepository.save(session);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("nextQuestionId", null);
            response.put("completed", true);
            response.put("isFollowUp", false);

            return ResponseEntity.ok(response);
        }

        String nextQuestionText = pickQuestionFromBank(nextTopicId, allAttempts);

        QuestionAttempt nextAttempt = QuestionAttempt.builder()
                .session(session)
                .questionText(nextQuestionText)
                .topic(topicRepository.findById(nextTopicId).orElse(null))
                .followupDepth(0)
                .build();
        nextAttempt = questionAttemptRepository.save(nextAttempt);

        session.setCurrentQuestionId(nextAttempt.getId());
        sessionRepository.save(session);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("nextQuestionId", nextAttempt.getId());
        response.put("nextQuestionText", nextQuestionText);
        response.put("topicId", nextAttempt.getTopic() != null ? nextAttempt.getTopic().getId() : null);
        response.put("completed", false);
        response.put("isFollowUp", false);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions/{sessionId}/report")
    public ResponseEntity<Map<String, Object>> getReport(@PathVariable UUID sessionId,
                                                         @CookieValue(value = "SESSION_EMPLOYEE", required = false) String cookieValue,
                                                         HttpServletRequest request) {
        Optional<Session> sessionOpt = tokenService.validateSessionCookie(request);
        if (sessionOpt.isEmpty() || !sessionOpt.get().getId().equals(sessionId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Session session = sessionOpt.get();
        if (!"COMPLETED".equals(session.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Session not completed"));
        }

        Map<String, Object> report = reportService.generateReport(sessionId);
        return ResponseEntity.ok(report);
    }

    /**
     * Pick a question from the admin-generated question bank for the given topic.
     * Returns the first question that hasn't been used yet in this session.
     * Throws if no questions exist in the bank for the topic.
     */
    private String pickQuestionFromBank(UUID topicId, List<QuestionAttempt> sessionAttempts) {
        List<QuestionBank> bankQuestions = questionBankRepository.findByTopicIdOrderBySortOrderAsc(topicId);

        if (bankQuestions.isEmpty()) {
            throw new IllegalStateException(
                    "Для темы нет сгенерированных вопросов. Администратор должен сгенерировать вопросы перед началом оценки.");
        }

        Set<String> usedTexts = sessionAttempts.stream()
                .filter(a -> a.getTopic() != null && a.getTopic().getId().equals(topicId))
                .map(QuestionAttempt::getQuestionText)
                .collect(Collectors.toSet());

        for (QuestionBank bankQuestion : bankQuestions) {
            if (!usedTexts.contains(bankQuestion.getQuestionText())) {
                return bankQuestion.getQuestionText();
            }
        }

        throw new IllegalStateException(
                "Все вопросы из банка для темы уже использованы. Администратор должен сгенерировать дополнительные вопросы.");
    }

    /**
     * A topic is considered "answered" only when an attempt exists for it that the employee
     * actually answered (non-empty final_transcript). Unanswered attempts do not consume a topic,
     * so reloads / StrictMode double-calls never mark the session completed prematurely.
     */
    private UUID findNextTopicId(List<QuestionAttempt> attempts, List<Topic> allTopics) {
        Set<UUID> answeredTopicIds = attempts.stream()
                .filter(a -> a.getTopic() != null)
                .filter(a -> a.getFinalTranscript() != null && !a.getFinalTranscript().isBlank())
                .map(a -> a.getTopic().getId())
                .collect(Collectors.toSet());

        return allTopics.stream()
                .map(Topic::getId)
                .filter(id -> !answeredTopicIds.contains(id))
                .findFirst()
                .orElse(null);
    }
}
