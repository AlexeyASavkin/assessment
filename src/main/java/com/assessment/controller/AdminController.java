package com.assessment.controller;

import com.assessment.dto.ApplicationSummary;
import com.assessment.entity.*;
import com.assessment.repository.*;
import com.assessment.security.HmacTokenValidator;
import com.assessment.service.AiProviderService;
import com.assessment.service.QuestionGeneratorService;
import com.assessment.service.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

/**
 * REST-контроллер для административных операций.
 * Обрабатывает все запросы по пути {@code /api/admin} и требует аутентификации с ролью ADMIN.
 * Управляет компетенциями, разделами, темами,
 * банком вопросов, сотрудниками и настройками ИИ.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final CompetencyRepository competencyRepository;
    private final SectionRepository sectionRepository;
    private final TopicRepository topicRepository;
    private final EmployeeRepository employeeRepository;
    private final AssessmentInviteTokenRepository tokenRepository;
    private final HmacTokenValidator hmacValidator;
    private final AiProviderService aiProviderService;
    private final QuestionGeneratorService questionGeneratorService;
    private final QuestionBankRepository questionBankRepository;
    private final SessionRepository sessionRepository;
    private final ReportService reportService;

    /**
     * Конструктор с внедрением зависимостей репозиториев и сервисов.
     *
     * @param competencyRepository      репозиторий компетенций
     * @param sectionRepository         репозиторий разделов
     * @param topicRepository           репозиторий тем
     * @param employeeRepository        репозиторий сотрудников
     * @param tokenRepository           репозиторий пригласительных токенов
     * @param hmacValidator             сервис валидации HMAC-токенов
     * @param aiProviderService         сервис переключения провайдера ИИ
     * @param questionGeneratorService  сервис генерации вопросов
     * @param questionBankRepository    репозиторий банка вопросов
     * @param sessionRepository         репозиторий сессий оценки
     * @param reportService             сервис формирования отчётов
     */
    public AdminController(CompetencyRepository competencyRepository,
                           SectionRepository sectionRepository,
                           TopicRepository topicRepository,
                           EmployeeRepository employeeRepository,
                           AssessmentInviteTokenRepository tokenRepository,
                           HmacTokenValidator hmacValidator,
                           AiProviderService aiProviderService,
                           QuestionGeneratorService questionGeneratorService,
                           QuestionBankRepository questionBankRepository,
                           com.assessment.repository.SessionRepository sessionRepository,
                           ReportService reportService) {
        this.competencyRepository = competencyRepository;
        this.sectionRepository = sectionRepository;
        this.topicRepository = topicRepository;
        this.employeeRepository = employeeRepository;
        this.tokenRepository = tokenRepository;
        this.hmacValidator = hmacValidator;
        this.aiProviderService = aiProviderService;
        this.questionGeneratorService = questionGeneratorService;
        this.questionBankRepository = questionBankRepository;
        this.sessionRepository = sessionRepository;
        this.reportService = reportService;
    }

    /**
     * Создает новую компетенцию.
     *
     * @param competency данные компетенции из тела запроса
     * @return созданная компетенция с HTTP 201
     */
    @PostMapping("/competencies")
    public ResponseEntity<Competency> createCompetency(@RequestBody Competency competency) {
        return ResponseEntity.status(HttpStatus.CREATED).body(competencyRepository.save(competency));
    }

    /**
     * Возвращает список всех компетенций.
     *
     * @return список компетенций с HTTP 200
     */
    @GetMapping("/competencies")
    public ResponseEntity<List<Competency>> listCompetencies() {
        return ResponseEntity.ok(competencyRepository.findAll());
    }

    /**
     * Возвращает компетенцию по идентификатору.
     *
     * @param id идентификатор компетенции
     * @return компетенция с HTTP 200 или HTTP 404, если не найдена
     */
    @GetMapping("/competencies/{id}")
    public ResponseEntity<Competency> getCompetency(@PathVariable UUID id) {
        return competencyRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Обновляет название и описание компетенции.
     *
     * @param id      идентификатор обновляемой компетенции
     * @param updated новые данные компетенции из тела запроса
     * @return обновленная компетенция с HTTP 200 или HTTP 404
     */
    @PutMapping("/competencies/{id}")
    public ResponseEntity<Competency> updateCompetency(@PathVariable UUID id, @RequestBody Competency updated) {
        return competencyRepository.findById(id)
                .map(competency -> {
                    competency.setName(updated.getName());
                    competency.setDescription(updated.getDescription());
                    return ResponseEntity.ok(competencyRepository.save(competency));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Удаляет компетенцию по идентификатору.
     *
     * @param id идентификатор удаляемой компетенции
     * @return HTTP 204 при успешном удалении
     */
    @DeleteMapping("/competencies/{id}")
    public ResponseEntity<Void> deleteCompetency(@PathVariable UUID id) {
        competencyRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Sections ----

    /**
     * Создает раздел внутри указанной компетенции.
     *
     * @param competencyId идентификатор компетенции
     * @param section      данные раздела из тела запроса
     * @return созданный раздел с HTTP 201 или HTTP 404, если компетенция не найдена
     */
    @PostMapping("/competencies/{competencyId}/sections")
    public ResponseEntity<Section> createSection(@PathVariable UUID competencyId, @RequestBody Section section) {
        return competencyRepository.findById(competencyId)
                .map(competency -> {
                    section.setCompetency(competency);
                    return ResponseEntity.status(HttpStatus.CREATED).body(sectionRepository.save(section));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Возвращает список разделов для указанной компетенции.
     *
     * @param competencyId идентификатор компетенции
     * @return список разделов с HTTP 200
     */
    @GetMapping("/competencies/{competencyId}/sections")
    public ResponseEntity<List<Section>> listSections(@PathVariable UUID competencyId) {
        return ResponseEntity.ok(sectionRepository.findByCompetencyId(competencyId));
    }

    /**
     * Обновляет название, описание и порядок сортировки раздела.
     *
     * @param id      идентификатор обновляемого раздела
     * @param updated новые данные раздела из тела запроса
     * @return обновленный раздел с HTTP 200 или HTTP 404
     */
    @PutMapping("/sections/{id}")
    public ResponseEntity<Section> updateSection(@PathVariable UUID id, @RequestBody Section updated) {
        return sectionRepository.findById(id)
                .map(section -> {
                    section.setName(updated.getName());
                    section.setDescription(updated.getDescription());
                    section.setSortOrder(updated.getSortOrder());
                    return ResponseEntity.ok(sectionRepository.save(section));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Удаляет раздел по идентификатору.
     *
     * @param id идентификатор удаляемого раздела
     * @return HTTP 204 при успешном удалении
     */
    @DeleteMapping("/sections/{id}")
    public ResponseEntity<Void> deleteSection(@PathVariable UUID id) {
        sectionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Topics ----

    /**
     * Создает тему внутри указанного раздела.
     *
     * @param sectionId идентификатор раздела
     * @param topic     данные темы из тела запроса
     * @return созданная тема с HTTP 201 или HTTP 404, если раздел не найден
     */
    @PostMapping("/sections/{sectionId}/topics")
    public ResponseEntity<Topic> createTopic(@PathVariable UUID sectionId, @RequestBody Topic topic) {
        return sectionRepository.findById(sectionId)
                .map(section -> {
                    topic.setSection(section);
                    return ResponseEntity.status(HttpStatus.CREATED).body(topicRepository.save(topic));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Возвращает список тем для указанного раздела.
     *
     * @param sectionId идентификатор раздела
     * @return список тем с HTTP 200
     */
    @GetMapping("/sections/{sectionId}/topics")
    public ResponseEntity<List<Topic>> listTopics(@PathVariable UUID sectionId) {
        return ResponseEntity.ok(topicRepository.findBySectionId(sectionId));
    }

    /**
     * Обновляет название, описание, вес и порядок сортировки темы.
     *
     * @param id      идентификатор обновляемой темы
     * @param updated новые данные темы из тела запроса
     * @return обновленная тема с HTTP 200 или HTTP 404
     */
    @PutMapping("/topics/{id}")
    public ResponseEntity<Topic> updateTopic(@PathVariable UUID id, @RequestBody Topic updated) {
        return topicRepository.findById(id)
                .map(topic -> {
                    topic.setName(updated.getName());
                    topic.setDescription(updated.getDescription());
                    topic.setWeight(updated.getWeight());
                    topic.setSortOrder(updated.getSortOrder());
                    return ResponseEntity.ok(topicRepository.save(topic));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Удаляет тему по идентификатору.
     *
     * @param id идентификатор удаляемой темы
     * @return HTTP 204 при успешном удалении
     */
    @DeleteMapping("/topics/{id}")
    public ResponseEntity<Void> deleteTopic(@PathVariable UUID id) {
        topicRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Topic Question Bank ----

    /**
     * Генерирует вопросы для указанной темы с помощью ИИ и сохраняет их в банк вопросов.
     *
     * @param topicId идентификатор темы
     * @param body    тело запроса с полями {@code count} (количество, 1–10) и {@code difficulty} (ALL, JUNIOR, MIDDLE, SENIOR)
     * @return список сгенерированных вопросов с HTTP 200,
     *         HTTP 404 если тема не найдена,
     *         HTTP 400 при неверных параметрах,
     *         HTTP 503 при ошибке генерации
     */
    @PostMapping("/topics/{topicId}/questions/generate")
    public ResponseEntity<?> generateTopicQuestions(@PathVariable UUID topicId,
                                                     @RequestBody Map<String, Object> body) {
        Object countObj = body.get("count");
        String difficulty = (String) body.getOrDefault("difficulty", "ALL");

        int count;
        try {
            count = countObj instanceof Number ? ((Number) countObj).intValue() : Integer.parseInt(countObj.toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "count должен быть числом от 1 до 10"));
        }

        if (count < 1 || count > 10) {
            return ResponseEntity.badRequest().body(Map.of("error", "count должен быть от 1 до 10"));
        }
        if (!VALID_DIFFICULTIES.contains(difficulty)) {
            return ResponseEntity.badRequest().body(Map.of("error", "difficulty должен быть: ALL, JUNIOR, MIDDLE или SENIOR"));
        }

        try {
            List<QuestionBank> questions = questionGeneratorService.generateAndSaveForTopic(topicId, count, difficulty);
            return ResponseEntity.ok(questions);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Ошибка генерации вопросов: " + e.getMessage()));
        }
    }

    /**
     * Возвращает список вопросов банка для указанной темы, отсортированных по порядку.
     *
     * @param topicId идентификатор темы
     * @return список вопросов с HTTP 200
     */
    @GetMapping("/topics/{topicId}/questions")
    public ResponseEntity<List<QuestionBank>> listTopicQuestions(@PathVariable UUID topicId) {
        return ResponseEntity.ok(questionBankRepository.findByTopicIdOrderBySortOrderAsc(topicId));
    }

    /**
     * Переупорядочивает вопросы банка для указанной темы.
     *
     * @param topicId   идентификатор темы
     * @param orderedIds список идентификаторов вопросов в новом порядке
     * @return HTTP 204 при успешном обновлении
     */
    @PutMapping("/topics/{topicId}/questions/reorder")
    public ResponseEntity<Void> reorderTopicQuestions(@PathVariable UUID topicId,
                                                      @RequestBody List<UUID> orderedIds) {
        List<QuestionBank> questions = questionBankRepository.findByTopicIdOrderBySortOrderAsc(topicId);
        for (int i = 0; i < orderedIds.size(); i++) {
            UUID id = orderedIds.get(i);
            int sortOrder = i;
            questions.stream()
                    .filter(q -> q.getId().equals(id))
                    .findFirst()
                    .ifPresent(q -> q.setSortOrder(sortOrder));
        }
        questionBankRepository.saveAll(questions);
        return ResponseEntity.noContent().build();
    }

    /**
     * Создает нового сотрудника. При указании компетенции разрешает ее по идентификатору.
     *
     * @param employee данные сотрудника из тела запроса
     * @return созданный сотрудник с HTTP 201
     */
    @PostMapping("/employees")
    public ResponseEntity<Employee> createEmployee(@RequestBody Employee employee) {
        // Resolve competency from ID if provided
        if (employee.getCompetency() != null && employee.getCompetency().getId() != null) {
            Competency comp = competencyRepository.findById(employee.getCompetency().getId())
                    .orElseThrow(() -> new NoSuchElementException("Компетенция не найдена: " + employee.getCompetency().getId()));
            employee.setCompetency(comp);
        } else {
            employee.setCompetency(null);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeRepository.save(employee));
    }

    /**
     * Возвращает список всех сотрудников, отсортированных по дате создания.
     *
     * @return список сотрудников с HTTP 200
     */
    @GetMapping("/employees")
    public ResponseEntity<List<Employee>> listEmployees() {
        return ResponseEntity.ok(employeeRepository.findAllByOrderByCreatedAtAsc());
    }

    /**
     * Возвращает сотрудника по идентификатору.
     *
     * @param id идентификатор сотрудника
     * @return сотрудник с HTTP 200 или HTTP 404, если не найден
     */
    @GetMapping("/employees/{id}")
    public ResponseEntity<Employee> getEmployee(@PathVariable UUID id) {
        return employeeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Обновляет данные сотрудника, включая ФИО, должность, отдел и компетенцию.
     *
     * @param id      идентификатор обновляемого сотрудника
     * @param updated новые данные сотрудника из тела запроса
     * @return обновленный сотрудник с HTTP 200 или HTTP 404
     */
    @PutMapping("/employees/{id}")
    public ResponseEntity<Employee> updateEmployee(@PathVariable UUID id, @RequestBody Employee updated) {
        return employeeRepository.findById(id)
                .map(employee -> {
                    employee.setFullName(updated.getFullName());
                    employee.setPosition(updated.getPosition());
                    employee.setDepartment(updated.getDepartment());
                    // Resolve competency from ID if provided
                    if (updated.getCompetency() != null && updated.getCompetency().getId() != null) {
                        Competency comp = competencyRepository.findById(updated.getCompetency().getId())
                                .orElseThrow(() -> new NoSuchElementException("Компетенция не найдена: " + updated.getCompetency().getId()));
                        employee.setCompetency(comp);
                    } else {
                        employee.setCompetency(null);
                    }
                    return ResponseEntity.ok(employeeRepository.save(employee));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Удаляет сотрудника вместе с его пригласительными токенами и сессиями.
     * Сессии каскадно удаляют попытки ответов через JPA CascadeType.ALL.
     *
     * @param id идентификатор удаляемого сотрудника
     * @return HTTP 204 при успешном удалении или HTTP 404
     */
    @Transactional
    @DeleteMapping("/employees/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable UUID id) {
        return employeeRepository.findById(id)
                .map(employee -> {
                    // Delete invite tokens first (FK → employees, no ON DELETE CASCADE)
                    tokenRepository.deleteByEmployeeId(employee.getId());
                    // Delete sessions (FK → employees, no ON DELETE CASCADE)
                    // Sessions cascade-delete question_attempts via JPA CascadeType.ALL
                    new java.util.ArrayList<>(sessionRepository.findByEmployeeId(employee.getId()))
                            .forEach(session -> sessionRepository.deleteById(session.getId()));
                    // Delete the employee
                    employeeRepository.delete(employee);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Генерирует одноразовую пригласительную ссылку для сотрудника.
     * Удаляет предыдущие токены этого сотрудника, чтобы избежать нарушения уникального ограничения.
     *
     * @param employeeId идентификатор сотрудника
     * @return пригласительный URL с HTTP 200 или HTTP 404, если сотрудник не найден
     */
    @Transactional
    @PostMapping("/employees/{employeeId}/invite")
    public ResponseEntity<String> generateInviteLink(@PathVariable UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .map(employee -> {
                    // Delete any existing tokens for this employee to avoid unique constraint violation
                    tokenRepository.deleteByEmployeeId(employeeId);

                    String token = hmacValidator.generateToken(employeeId.toString());
                    String hash = hmacValidator.generateToken(token);

                    AssessmentInviteToken inviteToken = AssessmentInviteToken.builder()
                            .tokenHash(hash)
                            .employee(employee)
                            .expiresAt(LocalDateTime.now().plusHours(72))
                            .build();
                    tokenRepository.save(inviteToken);

                    String inviteUrl = "/api/employee/invite/" + token;
                    return ResponseEntity.ok(inviteUrl);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Возвращает список всех выданных пригласительных токенов.
     *
     * @return список токенов с HTTP 200
     */
    @GetMapping("/tokens")
    public ResponseEntity<List<AssessmentInviteToken>> listTokens() {
        return ResponseEntity.ok(tokenRepository.findAll());
    }

    /**
     * Возвращает список всех заявок на оценку.
     * Заявка связывает пригласительный токен, сотрудника, сессию и агрегированные метрики результата.
     *
     * @return список заявок с HTTP 200
     */
    @GetMapping("/applications")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ApplicationSummary>> listApplications() {
        List<AssessmentInviteToken> tokens = tokenRepository.findAll();
        List<ApplicationSummary> summaries = new ArrayList<>();
        for (AssessmentInviteToken token : tokens) {
            Employee employee = token.getEmployee();
            Session session = token.getSession();
            UUID sessionId = session != null ? session.getId() : null;
            String sessionStatus = session != null ? session.getStatus() : null;
            LocalDateTime completedAt = session != null && "COMPLETED".equals(session.getStatus())
                    ? session.getUpdatedAt() : null;
            String competencyName = employee != null && employee.getCompetency() != null
                    ? employee.getCompetency().getName() : null;

            BigDecimal averageScore = null;
            String compositeLevel = null;
            if (sessionId != null && "COMPLETED".equals(sessionStatus)) {
                ReportService.SessionSummary summary = reportService.computeSummary(sessionId);
                averageScore = summary.averageScore();
                compositeLevel = summary.compositeLevel();
            }

            summaries.add(new ApplicationSummary(
                    token.getId(),
                    employee != null ? employee.getId() : null,
                    employee != null ? employee.getFullName() : null,
                    competencyName,
                    sessionStatus,
                    sessionId,
                    averageScore,
                    compositeLevel,
                    token.getCreatedAt(),
                    completedAt
            ));
        }
        return ResponseEntity.ok(summaries);
    }

    /**
     * Возвращает детальный отчёт по заявке.
     * Включает оценки по темам, все попытки ответов с текстом вопроса,
     * ответом сотрудника, оценкой и feedback, а также общую рекомендацию.
     *
     * @param sessionId идентификатор сессии
     * @return отчёт с HTTP 200 или HTTP 404, если сессия не найдена
     */
    @GetMapping("/applications/{sessionId}/report")
    public ResponseEntity<Map<String, Object>> getApplicationReport(@PathVariable UUID sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(reportService.generateAdminReport(sessionId));
    }

    /**
     * Возвращает текущие настройки провайдера ИИ и список доступных провайдеров.
     *
     * @return карта с полями {@code activeProvider} и {@code availableProviders} с HTTP 200
     */
    @GetMapping("/settings/ai")
    public ResponseEntity<Map<String, Object>> getAiSettings() {
        String activeProvider = aiProviderService.getActiveProvider();
        Map<String, Object> settings = new java.util.HashMap<>();
        settings.put("activeProvider", activeProvider);
        settings.put("availableProviders", java.util.List.of("gemini", "gigachat", "openrouter", "opencode"));
        return ResponseEntity.ok(settings);
    }

    /**
     * Обновляет активного провайдера ИИ.
     *
     * @param body тело запроса с полем {@code activeProvider}
     * @return обновленные настройки с HTTP 200 или HTTP 400 при отсутствии провайдера
     */
    @PutMapping("/settings/ai")
    public ResponseEntity<Map<String, Object>> updateAiSettings(@RequestBody Map<String, String> body) {
        String provider = body.get("activeProvider");
        if (provider == null || provider.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        aiProviderService.setActiveProvider(provider);
        return getAiSettings();
    }

    // ---- Question Bank ----

    /** Допустимые значения сложности для генерации вопросов. */
    private static final Set<String> VALID_DIFFICULTIES = Set.of("ALL", "JUNIOR", "MIDDLE", "SENIOR");

    /**
     * Генерирует вопросы для всех тем указанной компетенции с помощью ИИ.
     *
     * @param competencyId идентификатор компетенции
     * @param body         тело запроса с полями {@code count} (количество, 1–10) и {@code difficulty} (ALL, JUNIOR, MIDDLE, SENIOR)
     * @return список сгенерированных вопросов с HTTP 200,
     *         HTTP 404 если компетенция не найдена,
     *         HTTP 400 при неверных параметрах,
     *         HTTP 503 при ошибке генерации
     */
    @PostMapping("/competencies/{competencyId}/questions/generate")
    public ResponseEntity<?> generateQuestions(@PathVariable UUID competencyId,
                                               @RequestBody Map<String, Object> body) {
        Object countObj = body.get("count");
        String difficulty = (String) body.getOrDefault("difficulty", "ALL");

        int count;
        try {
            count = countObj instanceof Number ? ((Number) countObj).intValue() : Integer.parseInt(countObj.toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "count должен быть числом от 1 до 10"));
        }

        if (count < 1 || count > 10) {
            return ResponseEntity.badRequest().body(Map.of("error", "count должен быть от 1 до 10"));
        }
        if (!VALID_DIFFICULTIES.contains(difficulty)) {
            return ResponseEntity.badRequest().body(Map.of("error", "difficulty должен быть: ALL, JUNIOR, MIDDLE или SENIOR"));
        }

        try {
            List<QuestionBank> questions = questionGeneratorService.generateAndSave(competencyId, count, difficulty);
            return ResponseEntity.ok(questions);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Ошибка генерации вопросов: " + e.getMessage()));
        }
    }

    /**
     * Возвращает список вопросов банка для указанной компетенции, отсортированных по дате создания.
     *
     * @param competencyId идентификатор компетенции
     * @return список вопросов с HTTP 200
     */
    @GetMapping("/competencies/{competencyId}/questions")
    public ResponseEntity<List<QuestionBank>> listQuestions(@PathVariable UUID competencyId) {
        return ResponseEntity.ok(questionBankRepository.findByCompetencyIdOrderByCreatedAtDesc(competencyId));
    }

    /**
     * Обновляет текст вопроса в банке вопросов.
     *
     * @param id   идентификатор вопроса
     * @param body тело запроса с полем {@code questionText}
     * @return обновленный вопрос с HTTP 200, HTTP 404 если не найден, или HTTP 400 при пустом тексте
     */
    @PutMapping("/questions/{id}")
    public ResponseEntity<?> updateQuestion(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        String questionText = body.get("questionText");
        if (questionText == null || questionText.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "questionText обязателен"));
        }
        return questionBankRepository.findById(id)
                .map(question -> {
                    question.setQuestionText(questionText.trim());
                    return ResponseEntity.ok(questionBankRepository.save(question));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Удаляет вопрос из банка вопросов по идентификатору.
     *
     * @param id идентификатор удаляемого вопроса
     * @return HTTP 204 при успешном удалении
     */
    @DeleteMapping("/questions/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable UUID id) {
        questionBankRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
