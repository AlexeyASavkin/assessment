package com.assessment.controller;

import com.assessment.dto.*;
import com.assessment.entity.*;
import com.assessment.repository.*;
import com.assessment.security.EmployeeTokenService;
import com.assessment.service.FollowUpService;
import com.assessment.service.ReportService;
import com.assessment.service.ScoringService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST-контроллер для операций сотрудника.
 * Обрабатывает все запросы по пути {@code /api/employee} и требует
 * валидной сессионной cookie {@code SESSION_EMPLOYEE}.
 * Управляет прохождением оценки: получение вопросов, отправка ответов,
 * оценка через ИИ и формирование итогового отчета.
 */
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
    private final FollowUpService followUpService;

    /**
     * Конструктор с внедрением зависимостей сервисов и репозиториев.
     *
     * @param tokenService               сервис валидации токенов и сессий сотрудника
     * @param sessionRepository          репозиторий сессий оценки
     * @param scoringService             сервис оценки ответов через ИИ
     * @param questionAttemptRepository  репозиторий попыток ответов
     * @param questionBankRepository     репозиторий банка вопросов
     * @param topicRepository            репозиторий тем
     * @param reportService              сервис генерации отчетов
     * @param followUpService            сервис уточняющих вопросов для слабых ответов
     */
    public EmployeeController(EmployeeTokenService tokenService,
                              SessionRepository sessionRepository,
                              ScoringService scoringService,
                              QuestionAttemptRepository questionAttemptRepository,
                              QuestionBankRepository questionBankRepository,
                              TopicRepository topicRepository,
                              ReportService reportService,
                              FollowUpService followUpService) {
        this.tokenService = tokenService;
        this.sessionRepository = sessionRepository;
        this.scoringService = scoringService;
        this.questionAttemptRepository = questionAttemptRepository;
        this.questionBankRepository = questionBankRepository;
        this.topicRepository = topicRepository;
        this.reportService = reportService;
        this.followUpService = followUpService;
    }

    /**
     * Обрабатывает пригласительную ссылку сотрудника.
     * Проверяет HMAC-подпись токена, создает сессию, устанавливает cookie
     * и перенаправляет на страницу сессии или отчета, если сессия уже завершена.
     *
     * @param token    пригласительный токен из пути
     * @param request  HTTP-запрос
     * @param response HTTP-ответ для установки cookie и редиректа
     * @return HTTP 302 с заголовком {@code Location} или HTTP 403 при невалидном токене
     */
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

    /**
     * Возвращает текущую сессию сотрудника по cookie.
     * Если cookie отсутствует или невалидна, возвращает HTTP 401.
     *
     * @param cookieValue значение cookie SESSION_EMPLOYEE
     * @param request     HTTP-запрос для валидации сессии
     * @return текущая сессия с HTTP 200 или HTTP 401
     */
    @PostMapping("/sessions")
    public ResponseEntity<SessionDto> createSession(@CookieValue(value = "SESSION_EMPLOYEE", required = false) String cookieValue,
                                                     HttpServletRequest request) {
        Optional<com.assessment.entity.Session> sessionOpt = tokenService.validateSessionCookie(request);
        if (sessionOpt.isPresent()) {
            com.assessment.entity.Session session = sessionOpt.get();
            return ResponseEntity.ok(new SessionDto()
                    .id(session.getId())
                    .status(SessionDto.StatusEnum.fromValue(session.getStatus())));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    /**
     * Возвращает текущий вопрос для сессии или создает новый, если все еще нет.
     * Если сессия завершена, возвращает признак завершения.
     * Вопросы фильтруются по компетенции сотрудника.
     *
     * @param sessionId   идентификатор сессии
     * @param cookieValue значение cookie SESSION_EMPLOYEE
     * @param request     HTTP-запрос для валидации сессии
     * @return карта с данными вопроса и HTTP 200, HTTP 401 при несоответствии сессии,
     *         или признак завершения оценки
     */
    @GetMapping("/sessions/{sessionId}/questions")
    public ResponseEntity<?> getCurrentQuestion(@PathVariable UUID sessionId,
                                                  @CookieValue(value = "SESSION_EMPLOYEE", required = false) String cookieValue,
                                                  HttpServletRequest request) {
        Optional<com.assessment.entity.Session> sessionOpt = tokenService.validateSessionCookie(request);
        if (sessionOpt.isEmpty() || !sessionOpt.get().getId().equals(sessionId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        com.assessment.entity.Session session = sessionOpt.get();
        if ("COMPLETED".equals(session.getStatus())) {
            return ResponseEntity.ok(new CompletedResponseDto().completed(true));
        }

        UUID currentQuestionId = session.getCurrentQuestionId();
        if (currentQuestionId != null) {
            QuestionAttempt current = questionAttemptRepository.findById(currentQuestionId).orElse(null);
            if (current != null) {
                return ResponseEntity.ok(buildQuestionResponse(current));
            }
        }

        List<QuestionAttempt> attempts = questionAttemptRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);

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
            return ResponseEntity.ok(new CompletedResponseDto().completed(true));
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

        return ResponseEntity.ok(buildQuestionResponse(attempt));
    }

    /**
     * Формирует ответ с данными текущего вопроса для фронтенда.
     *
     * @param attempt попытка ответа с вопросом
     * @return ответ с данными вопроса
     */
    private QuestionResponseDto buildQuestionResponse(QuestionAttempt attempt) {
        return new QuestionResponseDto()
                .questionId(attempt.getId())
                .questionText(attempt.getQuestionText())
                .topicId(attempt.getTopic() != null ? attempt.getTopic().getId() : null)
                .isFollowUp(attempt.getFollowupDepth() > 0)
                .followupParentId(attempt.getFollowupParent() != null ? attempt.getFollowupParent().getId() : null);
    }

    /**
     * Принимает ответ сотрудника, оценивает его через ИИ и возвращает следующий вопрос
     * или признак завершения сессии. Если все темы пройдены, сессия переходит в статус COMPLETED.
     *
     * @param sessionId   идентификатор сессии
     * @param requestDto  тело запроса с полями {@code questionAttemptId}, {@code finalTranscript}
     * @param cookieValue значение cookie SESSION_EMPLOYEE
     * @param request     HTTP-запрос для валидации сессии
     * @return карта с данными следующего вопроса и HTTP 200, или HTTP 401 при несоответствии сессии
     */
    @PostMapping("/sessions/{sessionId}/answers")
    public ResponseEntity<?> submitAnswer(@PathVariable UUID sessionId,
                                            @RequestBody SubmitAnswerRequestDto requestDto,
                                            @CookieValue(value = "SESSION_EMPLOYEE", required = false) String cookieValue,
                                            HttpServletRequest request) {
        Optional<com.assessment.entity.Session> sessionOpt = tokenService.validateSessionCookie(request);
        if (sessionOpt.isEmpty() || !sessionOpt.get().getId().equals(sessionId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        com.assessment.entity.Session session = sessionOpt.get();
        UUID questionId = requestDto.getQuestionAttemptId();
        String finalTranscript = requestDto.getFinalTranscript();

        QuestionAttempt currentAttempt = questionAttemptRepository.findById(questionId).orElseThrow();

        currentAttempt.setFinalTranscript(finalTranscript);
        questionAttemptRepository.save(currentAttempt);

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

        UUID currentTopicId = currentAttempt.getTopic() != null ? currentAttempt.getTopic().getId() : null;
        int currentDepth = currentAttempt.getFollowupDepth() != null ? currentAttempt.getFollowupDepth() : 0;

        // ----- Филиализация ответа на уточняющий вопрос (depth=1) -----
        if (currentDepth > 0) {
            // Синхронно оцениваем сам уточняющий ответ
            scoringService.scoreAnswer(currentAttempt);

            // Переоцениваем родительскую основную попытку с учётом ответа на уточнение
            QuestionAttempt parent = currentAttempt.getFollowupParent();
            if (parent != null) {
                followUpService.rescoreMainAttempt(parent, currentAttempt);
            }

            // Ищем следующего кандидата на уточнение в той же теме
            if (currentTopicId != null) {
                QuestionAttempt candidate = followUpService.findFollowUpCandidate(sessionId, currentTopicId);
                if (candidate != null) {
                    String followUpQuestion = followUpService.generateFollowUpQuestion(candidate);
                    if (followUpQuestion != null && !followUpQuestion.isBlank()) {
                        QuestionAttempt followUpAttempt = QuestionAttempt.builder()
                                .session(session)
                                .questionText(followUpQuestion)
                                .topic(candidate.getTopic())
                                .followupDepth(currentDepth + 1)
                                .followupParent(candidate)
                                .build();
                        followUpAttempt = questionAttemptRepository.save(followUpAttempt);
                        session.setCurrentQuestionId(followUpAttempt.getId());
                        sessionRepository.save(session);
                        return ResponseEntity.ok(buildQuestionResponse(followUpAttempt));
                    }
                }
            }
            // Кандидатов нет — переходим к следующей теме
            return advanceToNextTopicOrComplete(session, topics);
        }

        // ----- Завершение темы: триггер уточняющих вопросов (depth=0) -----
        // Если это был последний вопрос темы — синхронно оцениваем все неоценённые ответы и ищем кандидата на уточнение.
        // Если не последний — берём следующий вопрос из банка (async scoring для UX).
        List<QuestionAttempt> allAttempts = questionAttemptRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        boolean topicHasMore = currentTopicId != null && hasUnusedQuestions(currentTopicId, allAttempts);

        if (topicHasMore) {
            // Тема не исчерпана — следующий вопрос из банка, async scoring для UX
            scoringService.scoreAnswerAsync(currentAttempt.getId());
            String nextQuestionText = pickQuestionFromBank(currentTopicId, allAttempts);
            QuestionAttempt nextAttempt = QuestionAttempt.builder()
                    .session(session)
                    .questionText(nextQuestionText)
                    .topic(topicRepository.findById(currentTopicId).orElse(null))
                    .followupDepth(0)
                    .build();
            nextAttempt = questionAttemptRepository.save(nextAttempt);
            session.setCurrentQuestionId(nextAttempt.getId());
            sessionRepository.save(session);

            return ResponseEntity.ok(new AnswerResponseDto()
                    .nextQuestionId(nextAttempt.getId())
                    .nextQuestionText(nextQuestionText)
                    .topicId(currentTopicId)
                    .completed(false)
                    .isFollowUp(false));
        }

        // Тема исчерпана — синхронно оцениваем все ответы сессии, затем ищем кандидата на уточнение
        scoringService.scoreUnscoredAttempts(sessionId);

        if (currentTopicId != null) {
            QuestionAttempt candidate = followUpService.findFollowUpCandidate(sessionId, currentTopicId);
            if (candidate != null) {
                String followUpQuestion = followUpService.generateFollowUpQuestion(candidate);
                if (followUpQuestion != null && !followUpQuestion.isBlank()) {
                    QuestionAttempt followUpAttempt = QuestionAttempt.builder()
                            .session(session)
                            .questionText(followUpQuestion)
                            .topic(candidate.getTopic())
                            .followupDepth(1)
                            .followupParent(candidate)
                            .build();
                    followUpAttempt = questionAttemptRepository.save(followUpAttempt);
                    session.setCurrentQuestionId(followUpAttempt.getId());
                    sessionRepository.save(session);
                    return ResponseEntity.ok(buildQuestionResponse(followUpAttempt));
                }
                // LLM-сбой — пропускаем кандидата, переходим к следующей теме
            }
        }

        // Уточнений нет — следующая тема или завершение
        return advanceToNextTopicOrComplete(session, topics);
    }

    /**
     * Подбирает следующий основной вопрос из следующей темы либо завершает сессию.
     *
     * @param session текущая сессия
     * @param topics  список тем компетенции сотрудника
     * @return ответ с следующим вопросом или признаком завершения
     */
    private ResponseEntity<?> advanceToNextTopicOrComplete(com.assessment.entity.Session session, List<Topic> topics) {
        List<QuestionAttempt> allAttempts = questionAttemptRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        UUID nextTopicId = findNextTopicId(allAttempts, topics);

        if (nextTopicId == null) {
            scoringService.scoreUnscoredAttempts(session.getId());
            session.setStatus("COMPLETED");
            sessionRepository.save(session);

            return ResponseEntity.ok(new CompletedResponseDto().completed(true));
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

        return ResponseEntity.ok(new AnswerResponseDto()
                .nextQuestionId(nextAttempt.getId())
                .nextQuestionText(nextQuestionText)
                .topicId(nextAttempt.getTopic() != null ? nextAttempt.getTopic().getId() : null)
                .completed(false)
                .isFollowUp(false));
    }

    /**
     * Возвращает итоговый отчет по завершенной сессии оценки.
     *
     * @param sessionId   идентификатор сессии
     * @param cookieValue значение cookie SESSION_EMPLOYEE
     * @param request     HTTP-запрос для валидации сессии
     * @return отчет с HTTP 200, HTTP 401 при несоответствии сессии,
     *         или HTTP 403 если сессия еще не завершена
     */
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
     * Выбирает вопрос из банка вопросов администратора для указанной темы.
     * Возвращает первый вопрос, который еще не использовался в текущей сессии.
     * Выбрасывает исключение, если для темы нет сгенерированных вопросов
     * или все вопросы уже использованы.
     *
     * @param topicId         идентификатор темы
     * @param sessionAttempts список уже заданных вопросов в сессии
     * @return текст выбранного вопроса
     * @throws IllegalStateException если вопросы для темы отсутствуют или исчерпаны
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
     * Проверяет, остались ли неиспользованные вопросы в банке для указанной темы.
     *
     * @param topicId         идентификатор темы
     * @param sessionAttempts список попыток ответов в сессии
     * @return true, если есть хотя бы один вопрос из банка, ещё не заданный в сессии
     */
    private boolean hasUnusedQuestions(UUID topicId, List<QuestionAttempt> sessionAttempts) {
        List<QuestionBank> bankQuestions = questionBankRepository.findByTopicIdOrderBySortOrderAsc(topicId);
        Set<String> usedTexts = sessionAttempts.stream()
                .filter(a -> a.getTopic() != null && a.getTopic().getId().equals(topicId))
                .map(QuestionAttempt::getQuestionText)
                .collect(Collectors.toSet());
        return bankQuestions.stream()
                .anyMatch(bq -> !usedTexts.contains(bq.getQuestionText()));
    }

    /**
     * Определяет следующую непройденную тему для сессии.
     * Тема считается пройденной только если существует попытка с непустым {@code finalTranscript}.
     * Неотвеченные попытки не расходуют тему, поэтому перезагрузки страницы
     * или двойные вызовы StrictMode не завершают сессию преждевременно.
     *
     * @param attempts список всех попыток в сессии
     * @param allTopics список всех доступных тем
     * @return идентификатор следующей темы или {@code null}, если все темы пройдены
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
