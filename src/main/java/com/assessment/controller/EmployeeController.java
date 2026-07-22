package com.assessment.controller;

import com.assessment.entity.*;
import com.assessment.repository.*;
import com.assessment.security.EmployeeTokenService;
import com.assessment.service.QuestionSelector;
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
    private final QuestionSelector questionSelector;
    private final ScoringService scoringService;
    private final QuestionAttemptRepository questionAttemptRepository;
    private final TopicRepository topicRepository;

    public EmployeeController(EmployeeTokenService tokenService,
                              SessionRepository sessionRepository,
                              QuestionSelector questionSelector,
                              ScoringService scoringService,
                              QuestionAttemptRepository questionAttemptRepository,
                              TopicRepository topicRepository) {
        this.tokenService = tokenService;
        this.sessionRepository = sessionRepository;
        this.questionSelector = questionSelector;
        this.scoringService = scoringService;
        this.questionAttemptRepository = questionAttemptRepository;
        this.topicRepository = topicRepository;
    }

    @GetMapping("/invite/{token}")
    public ResponseEntity<Void> handleInvite(@PathVariable String token,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        Optional<Session> sessionOpt = tokenService.validateInviteToken(token);
        if (sessionOpt.isPresent()) {
            tokenService.addSessionCookie(response, sessionOpt.get());
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "/session/" + sessionOpt.get().getId())
                    .build();
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
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
            return ResponseEntity.badRequest().body(Map.of("error", "Session completed"));
        }

        List<QuestionAttempt> attempts = questionAttemptRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        List<Topic> allTopics = topicRepository.findAll();

        UUID nextTopicId = findNextTopicId(attempts, allTopics);
        if (nextTopicId == null) {
            return ResponseEntity.ok(Map.of("completed", true));
        }

        String questionText = questionSelector.generateQuestion(session, nextTopicId);

        QuestionAttempt attempt = QuestionAttempt.builder()
                .session(session)
                .questionText(questionText)
                .topic(topicRepository.findById(nextTopicId).orElse(null))
                .followupDepth(0)
                .build();
        attempt = questionAttemptRepository.save(attempt);

        session.setCurrentQuestionId(attempt.getId());
        sessionRepository.save(session);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("questionId", attempt.getId());
        response.put("questionText", questionText);
        response.put("topicId", nextTopicId);
        response.put("isFollowUp", false);

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

        if (questionSelector.shouldAskFollowUp(session, currentAttempt.getTopic().getId())) {
            String followUpText = questionSelector.generateFollowUp(session, currentAttempt);

            QuestionAttempt followUp = QuestionAttempt.builder()
                    .session(session)
                    .questionText(followUpText)
                    .topic(currentAttempt.getTopic())
                    .followupDepth(1)
                    .followupParent(currentAttempt)
                    .build();
            followUp = questionAttemptRepository.save(followUp);

            session.setCurrentQuestionId(followUp.getId());
            sessionRepository.save(session);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("nextQuestionId", followUp.getId());
            response.put("completed", false);
            response.put("isFollowUp", true);
            response.put("followupParentId", currentAttempt.getId());

            return ResponseEntity.ok(response);
        }

        List<QuestionAttempt> allAttempts = questionAttemptRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        List<Topic> allTopics = topicRepository.findAll();
        UUID nextTopicId = findNextTopicId(allAttempts, allTopics);

        if (nextTopicId == null) {
            session.setStatus("COMPLETED");
            sessionRepository.save(session);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("nextQuestionId", null);
            response.put("completed", true);
            response.put("isFollowUp", false);

            return ResponseEntity.ok(response);
        }

        String nextQuestionText = questionSelector.generateQuestion(session, nextTopicId);

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
        response.put("completed", false);
        response.put("isFollowUp", false);

        return ResponseEntity.ok(response);
    }

    private UUID findNextTopicId(List<QuestionAttempt> attempts, List<Topic> allTopics) {
        Set<UUID> askedTopicIds = attempts.stream()
                .filter(a -> a.getTopic() != null)
                .map(a -> a.getTopic().getId())
                .collect(Collectors.toSet());

        return allTopics.stream()
                .map(Topic::getId)
                .filter(id -> !askedTopicIds.contains(id))
                .findFirst()
                .orElse(null);
    }
}
